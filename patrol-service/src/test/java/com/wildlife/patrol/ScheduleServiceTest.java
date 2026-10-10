package com.wildlife.patrol;

import com.wildlife.patrol.client.AllocationCreate;
import com.wildlife.patrol.client.AllocationDto;
import com.wildlife.patrol.client.AllocationStatusBody;
import com.wildlife.patrol.client.IncidentClient;
import com.wildlife.patrol.client.ResourceClient;
import com.wildlife.patrol.client.WildlifeClient;
import com.wildlife.patrol.dto.ScheduleRequest;
import com.wildlife.patrol.dto.ScheduleResponse;
import com.wildlife.patrol.entity.PatrolRoute;
import com.wildlife.patrol.entity.PatrolSchedule;
import com.wildlife.patrol.entity.PatrolStatus;
import com.wildlife.patrol.entity.PatrolTeam;
import com.wildlife.patrol.entity.Shift;
import com.wildlife.patrol.entity.TeamMember;
import com.wildlife.patrol.exception.ConflictException;
import com.wildlife.patrol.repository.LogRepository;
import com.wildlife.patrol.repository.ScheduleRepository;
import com.wildlife.patrol.service.RouteService;
import com.wildlife.patrol.service.ScheduleService;
import com.wildlife.patrol.service.TeamService;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Schedule creation reserves the team in resource-service (CONTRACTS.md 7.5). */
class ScheduleServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-10T04:00:00Z"), ZoneId.of("Asia/Kolkata"));

    private ScheduleRepository schedules;
    private ResourceClient resource;
    private ScheduleService service;

    @BeforeEach
    void setUp() {
        schedules = mock(ScheduleRepository.class);
        resource = mock(ResourceClient.class);
        TeamService teams = mock(TeamService.class);
        RouteService routes = mock(RouteService.class);
        service = new ScheduleService(schedules, mock(LogRepository.class), teams, routes, resource,
                mock(WildlifeClient.class), mock(IncidentClient.class), clock);

        PatrolTeam team = new PatrolTeam();
        team.setId(1L);
        team.setName("Mukki Tigers");
        team.setAreaId(1L);
        team.setActive(true);
        team.getMembers().add(new TeamMember(2L, "Ravi Uikey", "RANGE_OFFICER"));
        team.getMembers().add(new TeamMember(3L, "Mohan Das", "FOREST_GUARD"));
        PatrolRoute route = new PatrolRoute();
        route.setId(1L);
        route.setName("Mukki core loop");
        route.setAreaId(1L);
        route.setZoneId(1L);
        route.setZoneName("Mukki Core");

        when(teams.find(1L)).thenReturn(team);
        when(routes.find(1L)).thenReturn(route);
        when(schedules.saveAndFlush(any(PatrolSchedule.class))).thenAnswer(inv -> {
            PatrolSchedule s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });
    }

    @Test
    void reservesEveryMemberAndTheVehicle() {
        when(resource.createAllocation(any())).thenReturn(alloc(101L), alloc(102L), alloc(103L));

        ScheduleResponse r = service.create(request(7L));

        assertThat(r.status()).isEqualTo(PatrolStatus.SCHEDULED);
        assertThat(r.allocationIds()).containsExactly(101L, 102L, 103L);
        ArgumentCaptor<AllocationCreate> captor = ArgumentCaptor.forClass(AllocationCreate.class);
        verify(resource, times(3)).createAllocation(captor.capture());
        assertThat(captor.getAllValues()).extracting(AllocationCreate::resourceKind)
                .containsExactly("STAFF", "STAFF", "VEHICLE");
        assertThat(captor.getAllValues()).allMatch(a -> a.referenceId().equals(10L) && a.purpose().equals("PATROL"));
    }

    @Test
    void conflictFailsTheScheduleAndCancelsWhatWasReserved() {
        FeignException conflict = mock(FeignException.class);
        when(conflict.status()).thenReturn(409);
        when(conflict.contentUTF8()).thenReturn("{\"message\":\"Mohan Das is already allocated\"}");
        when(resource.createAllocation(any())).thenReturn(alloc(101L)).thenThrow(conflict);

        assertThatThrownBy(() -> service.create(request(null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Mohan Das is already allocated");
        verify(resource).changeAllocationStatus(eq(101L), eq(new AllocationStatusBody("CANCELLED")));
    }

    @Test
    void resourceServiceDownSavesScheduleWithWarning() {
        when(resource.createAllocation(any())).thenThrow(new RuntimeException("Connection refused"));

        ScheduleResponse r = service.create(request(null));

        assertThat(r.allocationIds()).isEmpty();
        assertThat(r.warnings()).anyMatch(w -> w.contains("NOT reserved"));
    }

    private static ScheduleRequest request(Long vehicleId) {
        return new ScheduleRequest(1L, 1L, LocalDate.of(2026, 10, 12), Shift.MORNING,
                LocalTime.of(6, 0), LocalTime.of(12, 0), vehicleId, null);
    }

    private static AllocationDto alloc(Long id) {
        return new AllocationDto(id, "STAFF", 2L, "x", "PATROL", 10L, null, null, "PLANNED");
    }
}

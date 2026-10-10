package com.wildlife.incident;

import com.wildlife.incident.client.HabitatClient;
import com.wildlife.incident.client.ZoneDto;
import com.wildlife.incident.dto.ZoneMonthlyIncidentReport;
import com.wildlife.incident.entity.Incident;
import com.wildlife.incident.entity.IncidentAssignment;
import com.wildlife.incident.entity.AssignmentRole;
import com.wildlife.incident.entity.IncidentStatus;
import com.wildlife.incident.entity.IncidentType;
import com.wildlife.incident.entity.Severity;
import com.wildlife.incident.repository.AssignmentRepository;
import com.wildlife.incident.repository.HistoryRepository;
import com.wildlife.incident.repository.IncidentRepository;
import com.wildlife.incident.service.IncidentService;
import com.wildlife.incident.service.ZoneMonthlyReportService;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The mandatory monthly zone review (CONTRACTS.md 7.6). */
class ZoneMonthlyReportServiceTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    /** 10 Oct 2026, 11:30 in India. */
    private static final Clock OCT_10 = Clock.fixed(Instant.parse("2026-10-10T06:00:00Z"), IST);

    private IncidentRepository incidents;
    private AssignmentRepository assignments;
    private HabitatClient habitat;

    @BeforeEach
    void setUp() {
        incidents = mock(IncidentRepository.class);
        assignments = mock(AssignmentRepository.class);
        habitat = mock(HabitatClient.class);
        when(habitat.getZone(1L)).thenReturn(new ZoneDto(1L, 1L, "Kanha Tiger Reserve", "Mukki Core", "KNH-MUK", "CORE"));
    }

    private ZoneMonthlyReportService service(Clock clock) {
        IncidentService incidentService = new IncidentService(incidents, assignments, mock(HistoryRepository.class),
                habitat, clock);
        return new ZoneMonthlyReportService(incidents, incidentService, habitat, clock);
    }

    @Test
    void queriesExactlyTheCurrentMonthWindow() {
        when(incidents.findInZoneBetween(any(), any(), any())).thenReturn(List.of());

        ZoneMonthlyIncidentReport r = service(OCT_10).currentMonth(1L, null);

        verify(incidents).findInZoneBetween(eq(1L), eq(LocalDateTime.of(2026, 10, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 11, 1, 0, 0)));
        assertThat(r.month()).isEqualTo("2026-10");
        assertThat(r.periodStart()).isEqualTo(LocalDateTime.of(2026, 10, 1, 0, 0));
        assertThat(r.periodEnd()).isEqualTo(LocalDateTime.of(2026, 11, 1, 0, 0));
    }

    @Test
    void monthIsDecidedInIndianTimeNotUtc() {
        // 31 Oct 2026 19:00 UTC is already 1 Nov 00:30 in India -> November
        Clock lateNight = Clock.fixed(Instant.parse("2026-10-31T19:00:00Z"), IST);
        when(incidents.findInZoneBetween(any(), any(), any())).thenReturn(List.of());

        ZoneMonthlyIncidentReport r = service(lateNight).currentMonth(1L, null);

        assertThat(r.month()).isEqualTo("2026-11");
    }

    @Test
    void summaryCountsAllStatusesWhileListIsFiltered() {
        when(incidents.findInZoneBetween(any(), any(), any())).thenReturn(List.of(
                incident(1L, IncidentStatus.IN_PROGRESS),
                incident(2L, IncidentStatus.VERIFIED),
                incident(3L, IncidentStatus.REPORTED),
                incident(4L, IncidentStatus.VERIFIED)));
        when(assignments.findActiveForIncidents(anyCollection())).thenReturn(List.of());

        ZoneMonthlyIncidentReport r = service(OCT_10).currentMonth(1L, IncidentStatus.VERIFIED);

        assertThat(r.incidents()).extracting(i -> i.id()).containsExactly(2L, 4L);
        assertThat(r.totalIncidents()).isEqualTo(4);
        assertThat(r.statusSummary()).containsEntry("VERIFIED", 2L).containsEntry("IN_PROGRESS", 1L)
                .containsEntry("REPORTED", 1L).containsEntry("RESOLVED", 0L).containsEntry("CLOSED", 0L);
        assertThat(r.statusFilter()).isEqualTo(IncidentStatus.VERIFIED);
        assertThat(r.zoneName()).isEqualTo("Mukki Core");
        assertThat(r.areaName()).isEqualTo("Kanha Tiger Reserve");
        assertThat(r.warnings()).isEmpty();
    }

    @Test
    void eachIncidentCarriesItsAssignedPersonnel() {
        Incident first = incident(1L, IncidentStatus.IN_PROGRESS);
        when(incidents.findInZoneBetween(any(), any(), any())).thenReturn(List.of(first));
        IncidentAssignment lead = new IncidentAssignment();
        lead.setId(11L);
        lead.setIncident(first);
        lead.setStaffId(2L);
        lead.setStaffName("Soni Kumari");
        lead.setDesignation("RANGE_OFFICER");
        lead.setAssignmentRole(AssignmentRole.LEAD);
        lead.setAssignedAt(LocalDateTime.of(2026, 10, 2, 9, 0));
        when(assignments.findActiveForIncidents(anyCollection())).thenReturn(List.of(lead));

        ZoneMonthlyIncidentReport r = service(OCT_10).currentMonth(1L, null);

        assertThat(r.incidents().get(0).assignedPersonnel())
                .singleElement()
                .satisfies(p -> {
                    assertThat(p.staffName()).isEqualTo("Soni Kumari");
                    assertThat(p.assignmentRole()).isEqualTo(AssignmentRole.LEAD);
                });
        assertThat(r.incidents().get(0).latitude()).isEqualTo(22.28);
    }

    @Test
    void habitatDownStillReturnsTheIncidentsWithAWarning() {
        when(habitat.getZone(1L)).thenThrow(new RuntimeException("Connection refused"));
        when(incidents.findInZoneBetween(any(), any(), any())).thenReturn(List.of(incident(1L, IncidentStatus.REPORTED)));
        when(assignments.findActiveForIncidents(anyCollection())).thenReturn(List.of());

        ZoneMonthlyIncidentReport r = service(OCT_10).currentMonth(1L, null);

        assertThat(r.incidents()).hasSize(1);
        assertThat(r.zoneName()).isEqualTo("Mukki Core (snapshot)");
        assertThat(r.warnings()).singleElement().asString().contains("habitat-service");
    }

    @Test
    void unknownZoneGivesEmptyListAndWarning() {
        FeignException notFound = mock(FeignException.class);
        when(notFound.status()).thenReturn(404);
        when(habitat.getZone(9L)).thenThrow(notFound);
        when(incidents.findInZoneBetween(any(), any(), any())).thenReturn(List.of());

        ZoneMonthlyIncidentReport r = service(OCT_10).currentMonth(9L, null);

        assertThat(r.incidents()).isEmpty();
        assertThat(r.totalIncidents()).isZero();
        assertThat(r.warnings()).containsExactly("Zone 9 does not exist in habitat-service");
    }

    private static Incident incident(Long id, IncidentStatus status) {
        Incident i = new Incident();
        i.setId(id);
        i.setIncidentCode("INC-2026-000" + id);
        i.setTitle("Incident " + id);
        i.setIncidentType(IncidentType.POACHING);
        i.setSeverity(Severity.HIGH);
        i.setStatus(status);
        i.setAreaId(1L);
        i.setZoneId(1L);
        i.setZoneName("Mukki Core (snapshot)");
        i.setLatitude(22.28);
        i.setLongitude(80.64);
        i.setOccurredAt(LocalDateTime.of(2026, 10, (int) (1 + id), 8, 0));
        return i;
    }
}

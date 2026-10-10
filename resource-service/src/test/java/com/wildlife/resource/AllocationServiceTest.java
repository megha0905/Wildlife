package com.wildlife.resource;

import com.wildlife.resource.dto.AllocationRequest;
import com.wildlife.resource.dto.AllocationResponse;
import com.wildlife.resource.entity.Allocation;
import com.wildlife.resource.entity.AllocationPurpose;
import com.wildlife.resource.entity.AllocationStatus;
import com.wildlife.resource.entity.ResourceKind;
import com.wildlife.resource.entity.ResourceStatus;
import com.wildlife.resource.entity.Staff;
import com.wildlife.resource.entity.StaffStatus;
import com.wildlife.resource.entity.Vehicle;
import com.wildlife.resource.exception.BusinessRuleException;
import com.wildlife.resource.exception.ConflictException;
import com.wildlife.resource.repository.AllocationRepository;
import com.wildlife.resource.repository.EquipmentRepository;
import com.wildlife.resource.repository.StaffRepository;
import com.wildlife.resource.repository.VehicleRepository;
import com.wildlife.resource.service.AllocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Allocation conflicts and transitions (CONTRACTS.md 6.4). */
class AllocationServiceTest {

    private static final LocalDateTime SIX = LocalDateTime.of(2026, 10, 12, 6, 0);
    private static final LocalDateTime NOON = LocalDateTime.of(2026, 10, 12, 12, 0);

    private AllocationRepository allocations;
    private StaffRepository staff;
    private VehicleRepository vehicles;
    private AllocationService service;

    @BeforeEach
    void setUp() {
        allocations = mock(AllocationRepository.class);
        staff = mock(StaffRepository.class);
        vehicles = mock(VehicleRepository.class);
        service = new AllocationService(allocations, staff, vehicles, mock(EquipmentRepository.class),
                Clock.systemDefaultZone());
        when(allocations.save(any(Allocation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(staff.findById(2L)).thenReturn(Optional.of(staff(2L, "Soni Kumari", StaffStatus.ACTIVE)));
    }

    @Test
    void freeResourceIsAllocatedAsPlanned() {
        when(allocations.findOverlapping(eq(ResourceKind.STAFF), eq(2L), eq(SIX), eq(NOON), anyCollection(), eq(-1L)))
                .thenReturn(List.of());

        AllocationResponse r = service.create(request(ResourceKind.STAFF, 2L, SIX, NOON));

        assertThat(r.status()).isEqualTo(AllocationStatus.PLANNED);
        assertThat(r.resourceLabel()).isEqualTo("Soni Kumari");
    }

    @Test
    void overlappingAllocationIsA409NamingTheClash() {
        Allocation existing = new Allocation();
        existing.setStartAt(LocalDateTime.of(2026, 10, 12, 10, 0));
        existing.setEndAt(LocalDateTime.of(2026, 10, 12, 14, 0));
        existing.setReferenceLabel("Patrol PS-7");
        when(allocations.findOverlapping(eq(ResourceKind.STAFF), eq(2L), eq(SIX), eq(NOON), anyCollection(), eq(-1L)))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.create(request(ResourceKind.STAFF, 2L, SIX, NOON)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Soni Kumari")
                .hasMessageContaining("Patrol PS-7");
        verify(allocations, never()).save(any());
    }

    @Test
    void onlyPlannedAndActiveAllocationsCountAsConflicts() {
        when(allocations.findOverlapping(any(), any(), any(), any(), anyCollection(), any())).thenAnswer(inv -> {
            Collection<AllocationStatus> statuses = inv.getArgument(4);
            assertThat(statuses).containsExactlyInAnyOrder(AllocationStatus.PLANNED, AllocationStatus.ACTIVE);
            return List.of();
        });

        service.create(request(ResourceKind.STAFF, 2L, SIX, NOON));
    }

    @Test
    void endBeforeStartIs422() {
        assertThatThrownBy(() -> service.create(request(ResourceKind.STAFF, 2L, NOON, SIX)))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.create(request(ResourceKind.STAFF, 2L, SIX, SIX)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void staffOnLeaveCannotBeAllocated() {
        when(staff.findById(8L)).thenReturn(Optional.of(staff(8L, "Lakshmi Pillai", StaffStatus.ON_LEAVE)));

        assertThatThrownBy(() -> service.create(request(ResourceKind.STAFF, 8L, SIX, NOON)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ON_LEAVE");
    }

    @Test
    void vehicleInMaintenanceCannotBeAllocated() {
        Vehicle van = new Vehicle();
        van.setId(3L);
        van.setRegistrationNumber("KL-06-G-0310");
        van.setStatus(ResourceStatus.MAINTENANCE);
        when(vehicles.findById(3L)).thenReturn(Optional.of(van));

        assertThatThrownBy(() -> service.create(request(ResourceKind.VEHICLE, 3L, SIX, NOON)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("MAINTENANCE");
    }

    @Test
    void unknownResourceIs422() {
        when(staff.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(ResourceKind.STAFF, 99L, SIX, NOON)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void updateExcludesItselfFromTheConflictCheck() {
        Allocation mine = new Allocation();
        mine.setId(5L);
        mine.setStatus(AllocationStatus.PLANNED);
        when(allocations.findById(5L)).thenReturn(Optional.of(mine));
        when(allocations.findOverlapping(any(), any(), any(), any(), anyCollection(), eq(5L))).thenReturn(List.of());

        service.update(5L, request(ResourceKind.STAFF, 2L, SIX, NOON.plusHours(1)));

        verify(allocations).findOverlapping(eq(ResourceKind.STAFF), eq(2L), eq(SIX), eq(NOON.plusHours(1)),
                anyCollection(), eq(5L));
    }

    @Test
    void legalAndIllegalStatusMoves() {
        Allocation a = new Allocation();
        a.setId(1L);
        a.setStatus(AllocationStatus.PLANNED);
        when(allocations.findById(1L)).thenReturn(Optional.of(a));

        assertThat(service.changeStatus(1L, AllocationStatus.ACTIVE).status()).isEqualTo(AllocationStatus.ACTIVE);
        assertThat(service.changeStatus(1L, AllocationStatus.COMPLETED).status()).isEqualTo(AllocationStatus.COMPLETED);
        assertThatThrownBy(() -> service.changeStatus(1L, AllocationStatus.ACTIVE))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void plannedCannotJumpToCompleted() {
        Allocation a = new Allocation();
        a.setId(2L);
        a.setStatus(AllocationStatus.PLANNED);
        when(allocations.findById(2L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.changeStatus(2L, AllocationStatus.COMPLETED))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("PLANNED to COMPLETED");
    }

    private static AllocationRequest request(ResourceKind kind, Long id, LocalDateTime start, LocalDateTime end) {
        return new AllocationRequest(kind, id, AllocationPurpose.PATROL, 1L, "Patrol PS-1", start, end, null);
    }

    private static Staff staff(Long id, String name, StaffStatus status) {
        Staff s = new Staff();
        s.setId(id);
        s.setFullName(name);
        s.setStatus(status);
        return s;
    }
}

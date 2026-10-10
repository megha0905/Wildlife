package com.wildlife.resource.service;

import com.wildlife.resource.config.AuthUser;
import com.wildlife.resource.config.CurrentUser;
import com.wildlife.resource.dto.AllocationRequest;
import com.wildlife.resource.dto.AllocationResponse;
import com.wildlife.resource.dto.AvailabilityResponse;
import com.wildlife.resource.dto.PageResponse;
import com.wildlife.resource.entity.Allocation;
import com.wildlife.resource.entity.AllocationPurpose;
import com.wildlife.resource.entity.AllocationStatus;
import com.wildlife.resource.entity.Equipment;
import com.wildlife.resource.entity.ResourceKind;
import com.wildlife.resource.entity.ResourceStatus;
import com.wildlife.resource.entity.Staff;
import com.wildlife.resource.entity.StaffStatus;
import com.wildlife.resource.entity.Vehicle;
import com.wildlife.resource.exception.BusinessRuleException;
import com.wildlife.resource.exception.ConflictException;
import com.wildlife.resource.exception.ResourceNotFoundException;
import com.wildlife.resource.repository.AllocationRepository;
import com.wildlife.resource.repository.EquipmentRepository;
import com.wildlife.resource.repository.StaffRepository;
import com.wildlife.resource.repository.VehicleRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.wildlife.resource.service.ResourceMapper.trimToNull;

/** Allocation rules: CONTRACTS.md 6.4. */
@Service
@Transactional
public class AllocationService {

    /** Statuses that still hold the resource. */
    static final Set<AllocationStatus> HOLDING = EnumSet.of(AllocationStatus.PLANNED, AllocationStatus.ACTIVE);

    /** Legal status moves. */
    static final Map<AllocationStatus, Set<AllocationStatus>> MOVES = Map.of(
            AllocationStatus.PLANNED, EnumSet.of(AllocationStatus.ACTIVE, AllocationStatus.CANCELLED),
            AllocationStatus.ACTIVE, EnumSet.of(AllocationStatus.COMPLETED, AllocationStatus.CANCELLED),
            AllocationStatus.COMPLETED, EnumSet.noneOf(AllocationStatus.class),
            AllocationStatus.CANCELLED, EnumSet.noneOf(AllocationStatus.class));

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final AllocationRepository allocationRepository;
    private final StaffRepository staffRepository;
    private final VehicleRepository vehicleRepository;
    private final EquipmentRepository equipmentRepository;
    private final Clock clock;

    public AllocationService(AllocationRepository allocationRepository, StaffRepository staffRepository,
                             VehicleRepository vehicleRepository, EquipmentRepository equipmentRepository,
                             Clock clock) {
        this.allocationRepository = allocationRepository;
        this.staffRepository = staffRepository;
        this.vehicleRepository = vehicleRepository;
        this.equipmentRepository = equipmentRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<AllocationResponse> search(ResourceKind kind, Long resourceId, AllocationPurpose purpose,
                                                   Long referenceId, AllocationStatus status,
                                                   LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Specification<Allocation> spec = Specification.where(null);
        if (kind != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("resourceKind"), kind));
        if (resourceId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("resourceId"), resourceId));
        if (purpose != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("purpose"), purpose));
        if (referenceId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("referenceId"), referenceId));
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        // window filters: allocations that overlap [from, to)
        if (from != null) spec = spec.and((root, query, cb) -> cb.greaterThan(root.get("endAt"), from));
        if (to != null) spec = spec.and((root, query, cb) -> cb.lessThan(root.get("startAt"), to));
        return PageResponse.of(allocationRepository.findAll(spec, pageable), ResourceMapper::toAllocation);
    }

    @Transactional(readOnly = true)
    public AllocationResponse get(Long id) {
        return ResourceMapper.toAllocation(find(id));
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(ResourceKind kind, Long resourceId, LocalDateTime start, LocalDateTime end) {
        requireValidWindow(start, end);
        List<AllocationResponse> conflicts = allocationRepository
                .findOverlapping(kind, resourceId, start, end, HOLDING, -1L).stream()
                .map(ResourceMapper::toAllocation).toList();
        return new AvailabilityResponse(kind, resourceId, start, end, conflicts.isEmpty(), conflicts);
    }

    public AllocationResponse create(AllocationRequest req) {
        requireValidWindow(req.startAt(), req.endAt());
        String label = resolveUsableResource(req.resourceKind(), req.resourceId());
        rejectOverlaps(req.resourceKind(), req.resourceId(), label, req.startAt(), req.endAt(), -1L);

        AuthUser user = CurrentUser.get();
        Allocation a = new Allocation();
        a.setResourceKind(req.resourceKind());
        a.setResourceId(req.resourceId());
        a.setResourceLabel(label);
        a.setStatus(AllocationStatus.PLANNED);
        a.setAllocatedByUserId(user.id());
        a.setAllocatedByName(user.displayName());
        a.setCreatedAt(LocalDateTime.now(clock));
        applyDetails(a, req);
        return ResourceMapper.toAllocation(allocationRepository.save(a));
    }

    public AllocationResponse update(Long id, AllocationRequest req) {
        Allocation a = find(id);
        if (!HOLDING.contains(a.getStatus())) {
            throw new ConflictException("Allocation " + id + " is " + a.getStatus() + " and can no longer be changed");
        }
        requireValidWindow(req.startAt(), req.endAt());
        String label = resolveUsableResource(req.resourceKind(), req.resourceId());
        rejectOverlaps(req.resourceKind(), req.resourceId(), label, req.startAt(), req.endAt(), id);
        a.setResourceKind(req.resourceKind());
        a.setResourceId(req.resourceId());
        a.setResourceLabel(label);
        applyDetails(a, req);
        return ResourceMapper.toAllocation(a);
    }

    public AllocationResponse changeStatus(Long id, AllocationStatus target) {
        Allocation a = find(id);
        if (!MOVES.get(a.getStatus()).contains(target)) {
            throw new ConflictException("Cannot move allocation " + id + " from " + a.getStatus() + " to " + target);
        }
        a.setStatus(target);
        return ResourceMapper.toAllocation(a);
    }

    // ---------------------------------------------------------------- rules

    /** endAt must be after startAt (422). */
    static void requireValidWindow(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new BusinessRuleException("endAt must be after startAt");
        }
    }

    /** Overlap with a PLANNED/ACTIVE allocation of the same resource -> 409 naming the clash. */
    void rejectOverlaps(ResourceKind kind, Long resourceId, String label, LocalDateTime start, LocalDateTime end,
                        Long excludeId) {
        List<Allocation> clashes = allocationRepository.findOverlapping(kind, resourceId, start, end, HOLDING, excludeId);
        if (!clashes.isEmpty()) {
            Allocation c = clashes.get(0);
            throw new ConflictException(label + " is already allocated from " + c.getStartAt().format(FMT)
                    + " to " + c.getEndAt().format(FMT)
                    + (c.getReferenceLabel() != null ? " (" + c.getReferenceLabel() + ")" : ""));
        }
    }

    /** The resource must exist (else 422) and be usable (else 422). Returns its display label. */
    String resolveUsableResource(ResourceKind kind, Long resourceId) {
        switch (kind) {
            case STAFF -> {
                Staff s = staffRepository.findById(resourceId)
                        .orElseThrow(() -> new BusinessRuleException("Staff member " + resourceId + " does not exist"));
                if (s.getStatus() != StaffStatus.ACTIVE) {
                    throw new BusinessRuleException(s.getFullName() + " is " + s.getStatus() + " and cannot be allocated");
                }
                return s.getFullName();
            }
            case VEHICLE -> {
                Vehicle v = vehicleRepository.findById(resourceId)
                        .orElseThrow(() -> new BusinessRuleException("Vehicle " + resourceId + " does not exist"));
                requireUsable(v.getStatus(), "Vehicle " + v.getRegistrationNumber());
                return v.getRegistrationNumber();
            }
            case EQUIPMENT -> {
                Equipment e = equipmentRepository.findById(resourceId)
                        .orElseThrow(() -> new BusinessRuleException("Equipment " + resourceId + " does not exist"));
                requireUsable(e.getStatus(), "Equipment " + e.getAssetTag());
                return e.getAssetTag() + " " + e.getName();
            }
            default -> throw new BusinessRuleException("Unknown resource kind " + kind);
        }
    }

    private static void requireUsable(ResourceStatus status, String what) {
        if (status == ResourceStatus.MAINTENANCE || status == ResourceStatus.RETIRED) {
            throw new BusinessRuleException(what + " is " + status + " and cannot be allocated");
        }
    }

    private static void applyDetails(Allocation a, AllocationRequest req) {
        a.setPurpose(req.purpose());
        a.setReferenceId(req.referenceId());
        a.setReferenceLabel(trimToNull(req.referenceLabel()));
        a.setStartAt(req.startAt());
        a.setEndAt(req.endAt());
        a.setNotes(trimToNull(req.notes()));
    }

    private Allocation find(Long id) {
        return allocationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Allocation", id));
    }
}

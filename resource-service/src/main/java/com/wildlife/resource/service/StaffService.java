package com.wildlife.resource.service;

import com.wildlife.resource.dto.PageResponse;
import com.wildlife.resource.dto.StaffRef;
import com.wildlife.resource.dto.StaffRequest;
import com.wildlife.resource.dto.StaffResponse;
import com.wildlife.resource.entity.Designation;
import com.wildlife.resource.entity.ResourceKind;
import com.wildlife.resource.entity.Staff;
import com.wildlife.resource.entity.StaffStatus;
import com.wildlife.resource.exception.ConflictException;
import com.wildlife.resource.exception.ResourceNotFoundException;
import com.wildlife.resource.repository.AllocationRepository;
import com.wildlife.resource.repository.StaffRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static com.wildlife.resource.service.ResourceMapper.trimToNull;

@Service
@Transactional
public class StaffService {

    private final StaffRepository staffRepository;
    private final AllocationRepository allocationRepository;

    public StaffService(StaffRepository staffRepository, AllocationRepository allocationRepository) {
        this.staffRepository = staffRepository;
        this.allocationRepository = allocationRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<StaffResponse> search(String q, Designation designation, StaffStatus status, Long areaId,
                                              Pageable pageable) {
        Specification<Staff> spec = Specification.where(null);
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), like),
                    cb.like(cb.lower(root.get("employeeCode")), like)));
        }
        if (designation != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("designation"), designation));
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (areaId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("homeAreaId"), areaId));
        return PageResponse.of(staffRepository.findAll(spec, pageable), ResourceMapper::toStaff);
    }

    @Transactional(readOnly = true)
    public StaffResponse get(Long id) {
        return ResourceMapper.toStaff(find(id));
    }

    /** Batch lookup used by patrol and programme services. Unknown ids are simply absent from the result. */
    @Transactional(readOnly = true)
    public List<StaffRef> lookup(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return staffRepository.findByIdIn(ids).stream()
                .sorted(Comparator.comparing(Staff::getId))
                .map(ResourceMapper::toStaffRef)
                .toList();
    }

    public StaffResponse create(StaffRequest req) {
        String code = req.employeeCode().trim().toUpperCase(Locale.ROOT);
        if (staffRepository.existsByEmployeeCodeIgnoreCase(code)) {
            throw new ConflictException("Employee code " + code + " is already in use");
        }
        Staff s = new Staff();
        apply(s, req, code);
        return ResourceMapper.toStaff(staffRepository.save(s));
    }

    public StaffResponse update(Long id, StaffRequest req) {
        Staff s = find(id);
        String code = req.employeeCode().trim().toUpperCase(Locale.ROOT);
        if (staffRepository.existsByEmployeeCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ConflictException("Employee code " + code + " is already in use");
        }
        apply(s, req, code);
        return ResourceMapper.toStaff(s);
    }

    public void delete(Long id) {
        Staff s = find(id);
        if (allocationRepository.existsByResourceKindAndResourceId(ResourceKind.STAFF, id)) {
            throw new ConflictException(s.getFullName() + " has allocations; set status INACTIVE instead of deleting");
        }
        staffRepository.delete(s);
    }

    Staff find(Long id) {
        return staffRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Staff", id));
    }

    private static void apply(Staff s, StaffRequest req, String code) {
        s.setEmployeeCode(code);
        s.setFullName(req.fullName().trim());
        s.setDesignation(req.designation());
        s.setStatus(req.status());
        s.setPhone(trimToNull(req.phone()));
        s.setEmail(trimToNull(req.email()));
        s.setHomeAreaId(req.homeAreaId());
        s.setJoinedOn(req.joinedOn());
    }
}

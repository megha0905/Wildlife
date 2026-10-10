package com.wildlife.incident.service;

import com.wildlife.incident.client.AllocationCreate;
import com.wildlife.incident.client.AllocationDto;
import com.wildlife.incident.client.AllocationStatusBody;
import com.wildlife.incident.client.RemoteErrors;
import com.wildlife.incident.client.ResourceClient;
import com.wildlife.incident.client.StaffDto;
import com.wildlife.incident.config.CurrentUser;
import com.wildlife.incident.dto.AssignmentRequest;
import com.wildlife.incident.dto.AssignmentResponse;
import com.wildlife.incident.entity.Incident;
import com.wildlife.incident.entity.IncidentAssignment;
import com.wildlife.incident.entity.IncidentStatus;
import com.wildlife.incident.exception.BusinessRuleException;
import com.wildlife.incident.exception.ConflictException;
import com.wildlife.incident.exception.ResourceNotFoundException;
import com.wildlife.incident.repository.AssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.wildlife.incident.service.IncidentMapper.trimToNull;

/**
 * Assigning field staff to incidents (CONTRACTS.md 7.6):
 * staff must exist in resource-service (404 -> 422; unreachable -> saved with warning),
 * and a 24-hour INCIDENT allocation is attempted (a clash only produces a warning).
 */
@Service
@Transactional
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private final AssignmentRepository assignmentRepository;
    private final IncidentService incidentService;
    private final ResourceClient resourceClient;
    private final Clock clock;

    public AssignmentService(AssignmentRepository assignmentRepository, IncidentService incidentService,
                             ResourceClient resourceClient, Clock clock) {
        this.assignmentRepository = assignmentRepository;
        this.incidentService = incidentService;
        this.resourceClient = resourceClient;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> list(Long incidentId) {
        incidentService.find(incidentId);
        return assignmentRepository.findByIncidentIdOrderByAssignedAtAsc(incidentId).stream()
                .map(a -> IncidentMapper.toAssignment(a, null)).toList();
    }

    public AssignmentResponse assign(Long incidentId, AssignmentRequest req) {
        Incident incident = incidentService.find(incidentId);
        if (incident.getStatus() == IncidentStatus.CLOSED) {
            throw new ConflictException("Incident " + incident.getIncidentCode() + " is CLOSED");
        }
        if (assignmentRepository.existsByIncidentIdAndStaffIdAndActiveTrue(incidentId, req.staffId())) {
            throw new ConflictException("Staff " + req.staffId() + " is already assigned to " + incident.getIncidentCode());
        }
        List<String> warnings = new ArrayList<>();
        StaffDto staff = null;
        try {
            staff = resourceClient.getStaff(req.staffId());
        } catch (Exception e) {
            if (RemoteErrors.isNotFound(e)) {
                throw new BusinessRuleException("Staff " + req.staffId() + " does not exist in resource-service");
            }
            log.warn("resource-service unavailable while assigning staff {}: {}", req.staffId(), e.getMessage());
            warnings.add("Staff " + req.staffId() + " could not be verified with resource-service; name omitted.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        IncidentAssignment a = new IncidentAssignment();
        a.setIncident(incident);
        a.setStaffId(req.staffId());
        a.setStaffName(staff != null ? staff.fullName() : null);
        a.setDesignation(staff != null ? staff.designation() : null);
        a.setAssignmentRole(req.assignmentRole());
        a.setAssignedAt(now);
        a.setAssignedByName(CurrentUser.get().displayName());
        a.setActive(true);
        a.setNote(trimToNull(req.note()));
        a.setAllocationId(tryAllocate(incident, req.staffId(), now, warnings));
        return IncidentMapper.toAssignment(assignmentRepository.save(a), warnings);
    }

    /** Sets the assignment inactive and releases its allocation (best effort). */
    public void unassign(Long incidentId, Long assignmentId) {
        IncidentAssignment a = assignmentRepository.findByIdAndIncidentId(assignmentId, incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment " + assignmentId
                        + " not found on incident " + incidentId));
        a.setActive(false);
        if (a.getAllocationId() != null) {
            try {
                resourceClient.changeAllocationStatus(a.getAllocationId(), new AllocationStatusBody("CANCELLED"));
            } catch (Exception e) {
                log.warn("Could not cancel allocation {}: {}", a.getAllocationId(), e.getMessage());
            }
        }
    }

    private Long tryAllocate(Incident incident, Long staffId, LocalDateTime now, List<String> warnings) {
        try {
            AllocationDto allocation = resourceClient.createAllocation(new AllocationCreate("STAFF", staffId,
                    "INCIDENT", incident.getId(), "Incident " + incident.getIncidentCode() + ": " + incident.getTitle(),
                    now, now.plusHours(24), null));
            return allocation.id();
        } catch (Exception e) {
            String reason = RemoteErrors.messageOf(e);
            if (RemoteErrors.isConflict(e)) {
                warnings.add("Assigned, but not reserved in resource-service: " + reason);
            } else {
                log.warn("Could not create allocation for staff {}: {}", staffId, reason);
                warnings.add("Assigned, but the allocation could not be created in resource-service (" + reason + ").");
            }
            return null;
        }
    }
}

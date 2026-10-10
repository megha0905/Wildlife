package com.wildlife.patrol.service;

import com.wildlife.patrol.client.AllocationCreate;
import com.wildlife.patrol.client.AllocationDto;
import com.wildlife.patrol.client.AllocationStatusBody;
import com.wildlife.patrol.client.AnimalDto;
import com.wildlife.patrol.client.IncidentClient;
import com.wildlife.patrol.client.IncidentCreate;
import com.wildlife.patrol.client.IncidentDto;
import com.wildlife.patrol.client.RemoteErrors;
import com.wildlife.patrol.client.ResourceClient;
import com.wildlife.patrol.client.SightingUpdate;
import com.wildlife.patrol.client.WildlifeClient;
import com.wildlife.patrol.config.AuthUser;
import com.wildlife.patrol.config.CurrentUser;
import com.wildlife.patrol.dto.EscalateRequest;
import com.wildlife.patrol.dto.LogRequest;
import com.wildlife.patrol.dto.LogResponse;
import com.wildlife.patrol.dto.PageResponse;
import com.wildlife.patrol.dto.ScheduleDetailResponse;
import com.wildlife.patrol.dto.ScheduleRequest;
import com.wildlife.patrol.dto.ScheduleResponse;
import com.wildlife.patrol.entity.PatrolLog;
import com.wildlife.patrol.entity.PatrolRoute;
import com.wildlife.patrol.entity.PatrolSchedule;
import com.wildlife.patrol.entity.PatrolStatus;
import com.wildlife.patrol.entity.PatrolTeam;
import com.wildlife.patrol.entity.TeamMember;
import com.wildlife.patrol.exception.BusinessRuleException;
import com.wildlife.patrol.exception.ConflictException;
import com.wildlife.patrol.exception.ForbiddenOperationException;
import com.wildlife.patrol.exception.ResourceNotFoundException;
import com.wildlife.patrol.exception.ServiceUnavailableException;
import com.wildlife.patrol.repository.LogRepository;
import com.wildlife.patrol.repository.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.wildlife.patrol.service.PatrolMapper.trimToNull;

/**
 * Patrol schedules and logs. Integration rules: CONTRACTS.md 7.5.
 * - Creating a schedule reserves every team member (and the vehicle) in resource-service.
 *   A 409/422 from resource-service fails the whole schedule; unreachable -> saved with a warning.
 * - Status changes move the allocations along (best effort, warnings on failure).
 * - Logs that mention an animal tag update the animal's last sighting in wildlife-service.
 * - Escalating a log creates an incident in incident-service (unreachable -> 503).
 */
@Service
@Transactional
public class ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);

    private final ScheduleRepository scheduleRepository;
    private final LogRepository logRepository;
    private final TeamService teamService;
    private final RouteService routeService;
    private final ResourceClient resourceClient;
    private final WildlifeClient wildlifeClient;
    private final IncidentClient incidentClient;
    private final Clock clock;

    public ScheduleService(ScheduleRepository scheduleRepository, LogRepository logRepository,
                           TeamService teamService, RouteService routeService, ResourceClient resourceClient,
                           WildlifeClient wildlifeClient, IncidentClient incidentClient, Clock clock) {
        this.scheduleRepository = scheduleRepository;
        this.logRepository = logRepository;
        this.teamService = teamService;
        this.routeService = routeService;
        this.resourceClient = resourceClient;
        this.wildlifeClient = wildlifeClient;
        this.incidentClient = incidentClient;
        this.clock = clock;
    }

    // ================================================================ queries

    @Transactional(readOnly = true)
    public PageResponse<ScheduleResponse> search(Long teamId, Long routeId, Long zoneId, PatrolStatus status,
                                                 LocalDate from, LocalDate to, Pageable pageable) {
        Specification<PatrolSchedule> spec = Specification.where(null);
        if (teamId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("team").get("id"), teamId));
        if (routeId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("route").get("id"), routeId));
        if (zoneId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("route").get("zoneId"), zoneId));
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (from != null) spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("patrolDate"), from));
        if (to != null) spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("patrolDate"), to));
        return PageResponse.of(scheduleRepository.findAll(spec, pageable), s -> PatrolMapper.toSchedule(s));
    }

    @Transactional(readOnly = true)
    public ScheduleDetailResponse get(Long id) {
        PatrolSchedule s = find(id);
        List<LogResponse> logs = logRepository.findByScheduleIdOrderByLoggedAtAsc(id).stream()
                .map(l -> PatrolMapper.toLog(l)).toList();
        return ScheduleDetailResponse.of(PatrolMapper.toSchedule(s), PatrolMapper.toTeam(s.getTeam()),
                PatrolMapper.toRoute(s.getRoute()), logs);
    }

    @Transactional(readOnly = true)
    public List<LogResponse> logs(Long scheduleId) {
        find(scheduleId);
        return logRepository.findByScheduleIdOrderByLoggedAtAsc(scheduleId).stream()
                .map(l -> PatrolMapper.toLog(l)).toList();
    }

    // ================================================================ schedule commands

    public ScheduleResponse create(ScheduleRequest req) {
        PatrolSchedule s = new PatrolSchedule();
        s.setStatus(PatrolStatus.SCHEDULED);
        s.setCreatedByName(CurrentUser.get().displayName());
        apply(s, req);
        s = scheduleRepository.saveAndFlush(s); // we need the id as the allocation reference
        List<String> warnings = new ArrayList<>();
        s.getAllocationIds().addAll(allocate(s, warnings));
        return PatrolMapper.toSchedule(s, warnings);
    }

    public ScheduleResponse update(Long id, ScheduleRequest req) {
        PatrolSchedule s = find(id);
        if (s.getStatus() != PatrolStatus.SCHEDULED) {
            throw new ConflictException("Patrol " + PatrolMapper.code(s) + " is " + s.getStatus()
                    + " and can no longer be edited");
        }
        List<String> warnings = new ArrayList<>();
        // release the old reservations, then reserve again for the new window/team
        moveAllocations(s, "CANCELLED", warnings);
        s.getAllocationIds().clear();
        apply(s, req);
        s.getAllocationIds().addAll(allocate(s, warnings));
        return PatrolMapper.toSchedule(s, warnings);
    }

    public ScheduleResponse changeStatus(Long id, PatrolStatus target, String note) {
        PatrolSchedule s = find(id);
        PatrolWorkflow.requireMove(PatrolMapper.code(s), s.getStatus(), target);
        List<String> warnings = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(clock);
        switch (target) {
            case IN_PROGRESS -> {
                s.setStartedAt(now);
                moveAllocations(s, "ACTIVE", warnings);
            }
            case COMPLETED -> {
                s.setCompletedAt(now);
                moveAllocations(s, "COMPLETED", warnings);
            }
            case CANCELLED, MISSED -> moveAllocations(s, "CANCELLED", warnings);
            default -> { }
        }
        s.setStatus(target);
        if (trimToNull(note) != null) {
            String objective = s.getObjective() == null ? "" : s.getObjective() + " | ";
            String combined = objective + target + ": " + note.trim();
            s.setObjective(combined.length() > 500 ? combined.substring(0, 500) : combined);
        }
        return PatrolMapper.toSchedule(s, warnings);
    }

    // ================================================================ logs

    public LogResponse addLog(Long scheduleId, LogRequest req) {
        PatrolSchedule s = find(scheduleId);
        if (!PatrolWorkflow.acceptsLogs(s.getStatus())) {
            throw new BusinessRuleException("Logs can only be added to a patrol that is IN_PROGRESS or COMPLETED (patrol "
                    + PatrolMapper.code(s) + " is " + s.getStatus() + ")");
        }
        if (req.loggedAt().isAfter(LocalDateTime.now(clock).plusMinutes(10))) {
            throw new BusinessRuleException("loggedAt cannot be in the future");
        }
        AuthUser user = CurrentUser.get();
        PatrolLog entry = new PatrolLog();
        entry.setSchedule(s);
        entry.setLoggedAt(req.loggedAt());
        entry.setObservationType(req.observationType());
        entry.setLatitude(req.latitude());
        entry.setLongitude(req.longitude());
        entry.setDescription(req.description().trim());
        entry.setAnimalTagId(trimToNull(req.animalTagId()) == null ? null : req.animalTagId().trim().toUpperCase(Locale.ROOT));
        entry.setSeverityHint(req.severityHint());
        entry.setReportedByUserId(user.id());
        entry.setReportedByName(user.displayName());
        entry.setCreatedAt(LocalDateTime.now(clock));
        PatrolLog saved = logRepository.save(entry);

        List<String> warnings = new ArrayList<>();
        if (saved.getAnimalTagId() != null) {
            recordSighting(s, saved, warnings);
        }
        return PatrolMapper.toLog(saved, warnings);
    }

    public LogResponse escalate(Long logId, EscalateRequest req) {
        PatrolLog entry = logRepository.findWithScheduleById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("Patrol log", logId));
        if (entry.getIncidentId() != null) {
            throw new ConflictException("Log " + logId + " was already escalated to incident " + entry.getIncidentId());
        }
        PatrolSchedule s = entry.getSchedule();
        PatrolRoute route = s.getRoute();
        IncidentCreate body = new IncidentCreate(
                req.title().trim(), req.incidentType(), req.severity().name(), entry.getDescription(),
                route.getAreaId(), route.getZoneId(), entry.getLatitude(), entry.getLongitude(),
                "Reported by patrol " + PatrolMapper.code(s) + " on route " + route.getName(),
                entry.getLoggedAt(), entry.getId());
        try {
            IncidentDto incident = incidentClient.createIncident(body);
            entry.setIncidentId(incident.id());
        } catch (Exception e) {
            if (e instanceof feign.FeignException fe && fe.status() == 403) {
                throw new ForbiddenOperationException("incident-service refused: " + RemoteErrors.messageOf(e));
            }
            if (RemoteErrors.isClientError(e)) {
                throw new BusinessRuleException("incident-service rejected the incident: " + RemoteErrors.messageOf(e));
            }
            log.warn("incident-service unavailable while escalating log {}: {}", logId, e.getMessage());
            throw new ServiceUnavailableException("incident-service is unavailable; try escalating again later");
        }
        return PatrolMapper.toLog(entry);
    }

    // ================================================================ helpers

    PatrolSchedule find(Long id) {
        return scheduleRepository.findWithTeamAndRouteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patrol schedule", id));
    }

    private void apply(PatrolSchedule s, ScheduleRequest req) {
        PatrolWorkflow.requireValidTimes(req.shift(), req.startTime(), req.endTime());
        PatrolTeam team = teamService.find(req.teamId());
        if (!team.isActive()) {
            throw new BusinessRuleException("Team " + team.getName() + " is not active");
        }
        PatrolRoute route = routeService.find(req.routeId());
        if (!route.getAreaId().equals(team.getAreaId())) {
            throw new BusinessRuleException("Route " + route.getName() + " is in area " + route.getAreaId()
                    + " but team " + team.getName() + " belongs to area " + team.getAreaId());
        }
        if (req.patrolDate().isBefore(LocalDate.now(clock).minusDays(1))) {
            throw new BusinessRuleException("Patrols cannot be scheduled in the past");
        }
        s.setTeam(team);
        s.setRoute(route);
        s.setPatrolDate(req.patrolDate());
        s.setShift(req.shift());
        s.setStartTime(req.startTime());
        s.setEndTime(req.endTime());
        s.setVehicleId(req.vehicleId());
        s.setObjective(trimToNull(req.objective()));
    }

    /**
     * Reserves every team member and the vehicle. Returns the created allocation ids.
     * 409/422 from resource-service: undo what was reserved and fail the request.
     * resource-service unreachable: keep the schedule, add a warning, reserve nothing.
     */
    private List<Long> allocate(PatrolSchedule s, List<String> warnings) {
        String label = "Patrol " + PatrolMapper.code(s) + ", " + nameOr(s.getRoute().getZoneName(), "zone " + s.getRoute().getZoneId())
                + ", " + s.getPatrolDate().format(DAY) + " " + s.getShift().name().toLowerCase(Locale.ROOT);
        List<AllocationCreate> requests = new ArrayList<>();
        for (TeamMember m : s.getTeam().getMembers()) {
            requests.add(new AllocationCreate("STAFF", m.getStaffId(), "PATROL", s.getId(), label,
                    s.windowStart(), s.windowEnd(), null));
        }
        if (s.getVehicleId() != null) {
            requests.add(new AllocationCreate("VEHICLE", s.getVehicleId(), "PATROL", s.getId(), label,
                    s.windowStart(), s.windowEnd(), null));
        }

        List<Long> created = new ArrayList<>();
        for (AllocationCreate body : requests) {
            try {
                AllocationDto a = resourceClient.createAllocation(body);
                created.add(a.id());
            } catch (Exception e) {
                if (RemoteErrors.isClientError(e)) {
                    cancelQuietly(created);
                    String reason = RemoteErrors.messageOf(e);
                    if (RemoteErrors.isConflict(e)) {
                        throw new ConflictException("Cannot schedule patrol: " + reason);
                    }
                    throw new BusinessRuleException("Cannot schedule patrol: " + reason);
                }
                log.warn("resource-service unavailable while allocating patrol {}: {}", s.getId(), e.getMessage());
                cancelQuietly(created);
                warnings.add("resource-service is unavailable; team and vehicle were NOT reserved for this patrol.");
                return List.of();
            }
        }
        return created;
    }

    private void moveAllocations(PatrolSchedule s, String status, List<String> warnings) {
        for (Long allocationId : s.getAllocationIds()) {
            try {
                resourceClient.changeAllocationStatus(allocationId, new AllocationStatusBody(status));
            } catch (Exception e) {
                log.warn("Could not set allocation {} to {}: {}", allocationId, status, e.getMessage());
                warnings.add("Allocation " + allocationId + " could not be set to " + status + ": "
                        + RemoteErrors.messageOf(e));
            }
        }
    }

    private void cancelQuietly(List<Long> allocationIds) {
        for (Long id : allocationIds) {
            try {
                resourceClient.changeAllocationStatus(id, new AllocationStatusBody("CANCELLED"));
            } catch (Exception e) {
                log.warn("Could not roll back allocation {}: {}", id, e.getMessage());
            }
        }
    }

    private void recordSighting(PatrolSchedule s, PatrolLog entry, List<String> warnings) {
        try {
            AnimalDto animal = wildlifeClient.getByTag(entry.getAnimalTagId());
            String note = entry.getDescription().length() > 200
                    ? entry.getDescription().substring(0, 200) : entry.getDescription();
            wildlifeClient.recordSighting(animal.id(), new SightingUpdate(entry.getLoggedAt(), entry.getLatitude(),
                    entry.getLongitude(), s.getRoute().getZoneId(), s.getRoute().getAreaId(), null,
                    "PATROL:" + PatrolMapper.code(s), note));
        } catch (Exception e) {
            if (RemoteErrors.isNotFound(e)) {
                warnings.add("No animal with tag " + entry.getAnimalTagId() + " in wildlife-service; sighting not linked.");
            } else {
                log.warn("Could not record sighting of {}: {}", entry.getAnimalTagId(), e.getMessage());
                warnings.add("Sighting of " + entry.getAnimalTagId() + " could not be sent to wildlife-service: "
                        + RemoteErrors.messageOf(e));
            }
        }
    }

    private static String nameOr(String name, String fallback) {
        return name != null ? name : fallback;
    }
}

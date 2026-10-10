package com.wildlife.incident.service;

import com.wildlife.incident.client.HabitatClient;
import com.wildlife.incident.client.RemoteErrors;
import com.wildlife.incident.client.ZoneDto;
import com.wildlife.incident.config.AuthUser;
import com.wildlife.incident.config.CurrentUser;
import com.wildlife.incident.dto.AssignedPersonnel;
import com.wildlife.incident.dto.IncidentDetailResponse;
import com.wildlife.incident.dto.IncidentRequest;
import com.wildlife.incident.dto.IncidentResponse;
import com.wildlife.incident.dto.PageResponse;
import com.wildlife.incident.dto.StatusHistoryResponse;
import com.wildlife.incident.entity.Incident;
import com.wildlife.incident.entity.IncidentAssignment;
import com.wildlife.incident.entity.IncidentStatus;
import com.wildlife.incident.entity.IncidentStatusHistory;
import com.wildlife.incident.entity.IncidentType;
import com.wildlife.incident.entity.Severity;
import com.wildlife.incident.exception.BusinessRuleException;
import com.wildlife.incident.exception.ConflictException;
import com.wildlife.incident.exception.ResourceNotFoundException;
import com.wildlife.incident.repository.AssignmentRepository;
import com.wildlife.incident.repository.HistoryRepository;
import com.wildlife.incident.repository.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.wildlife.incident.service.IncidentMapper.trimToNull;

@Service
@Transactional
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;
    private final AssignmentRepository assignmentRepository;
    private final HistoryRepository historyRepository;
    private final HabitatClient habitatClient;
    private final Clock clock;

    public IncidentService(IncidentRepository incidentRepository, AssignmentRepository assignmentRepository,
                           HistoryRepository historyRepository, HabitatClient habitatClient, Clock clock) {
        this.incidentRepository = incidentRepository;
        this.assignmentRepository = assignmentRepository;
        this.historyRepository = historyRepository;
        this.habitatClient = habitatClient;
        this.clock = clock;
    }

    // ================================================================ queries

    @Transactional(readOnly = true)
    public PageResponse<IncidentResponse> search(IncidentType type, Severity severity, IncidentStatus status,
                                                 Long areaId, Long zoneId, LocalDate from, LocalDate to, String q,
                                                 Pageable pageable) {
        Specification<Incident> spec = Specification.where(null);
        if (type != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("incidentType"), type));
        if (severity != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("severity"), severity));
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (areaId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("areaId"), areaId));
        if (zoneId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("zoneId"), zoneId));
        if (from != null) {
            LocalDateTime start = from.atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), start));
        }
        if (to != null) {
            LocalDateTime end = to.plusDays(1).atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("occurredAt"), end));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("incidentCode")), like),
                    cb.like(cb.lower(root.get("description")), like)));
        }
        Page<Incident> page = incidentRepository.findAll(spec, pageable);
        Map<Long, List<AssignedPersonnel>> personnel = personnelFor(page.getContent().stream().map(Incident::getId).toList());
        return PageResponse.of(page, i -> IncidentMapper.toIncident(i, personnel.getOrDefault(i.getId(), List.of()), null));
    }

    @Transactional(readOnly = true)
    public IncidentDetailResponse get(Long id) {
        Incident i = find(id);
        List<StatusHistoryResponse> history = history(id);
        return IncidentDetailResponse.of(IncidentMapper.toIncident(i, personnelFor(List.of(id)).getOrDefault(id, List.of()), null),
                history);
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> history(Long id) {
        find(id);
        return historyRepository.findByIncidentIdOrderByChangedAtAscIdAsc(id).stream()
                .map(IncidentMapper::toHistory).toList();
    }

    // ================================================================ commands

    public IncidentResponse create(IncidentRequest req) {
        requireNotFuture(req.occurredAt());
        List<String> warnings = new ArrayList<>();
        String zoneName = checkZone(req.zoneId(), req.areaId(), warnings);

        AuthUser user = CurrentUser.get();
        LocalDateTime now = LocalDateTime.now(clock);
        Incident i = new Incident();
        i.setIncidentCode(String.format("INC-%d-%04d", now.getYear(), incidentRepository.nextCodeNumber()));
        i.setStatus(IncidentStatus.REPORTED);
        i.setReportedAt(now);
        i.setReportedByUserId(user.id());
        i.setReportedByName(user.displayName());
        apply(i, req, zoneName);
        Incident saved = incidentRepository.save(i);
        writeHistory(saved, null, IncidentStatus.REPORTED, user, "Incident reported");
        return IncidentMapper.toIncident(saved, List.of(), warnings);
    }

    public IncidentResponse update(Long id, IncidentRequest req) {
        Incident i = find(id);
        if (i.getStatus() == IncidentStatus.CLOSED) {
            throw new ConflictException("Incident " + i.getIncidentCode() + " is CLOSED and can no longer be edited");
        }
        requireNotFuture(req.occurredAt());
        List<String> warnings = new ArrayList<>();
        String zoneName = i.getZoneName();
        if (!req.zoneId().equals(i.getZoneId()) || !req.areaId().equals(i.getAreaId())) {
            zoneName = checkZone(req.zoneId(), req.areaId(), warnings);
        }
        apply(i, req, zoneName);
        return IncidentMapper.toIncident(i, personnelFor(List.of(id)).getOrDefault(id, List.of()), warnings);
    }

    /** Workflow move (CONTRACTS.md 6.1). */
    public IncidentResponse transition(Long id, IncidentStatus target, String note) {
        Incident i = find(id);
        AuthUser user = CurrentUser.get();
        long activeAssignments = assignmentRepository.countByIncidentIdAndActiveTrue(id);
        IncidentWorkflow.check(i.getIncidentCode(), i.getStatus(), target, user.roles(), activeAssignments, note);

        IncidentStatus from = i.getStatus();
        LocalDateTime now = LocalDateTime.now(clock);
        switch (target) {
            case VERIFIED -> i.setVerifiedAt(now);
            case RESOLVED -> {
                i.setResolvedAt(now);
                i.setResolutionNotes(note.trim());
            }
            case CLOSED -> i.setClosedAt(now);
            default -> { }
        }
        i.setStatus(target);
        writeHistory(i, from, target, user, trimToNull(note));
        return IncidentMapper.toIncident(i, personnelFor(List.of(id)).getOrDefault(id, List.of()), null);
    }

    /** Only ADMIN, only while REPORTED (mistaken reports). */
    public void delete(Long id) {
        Incident i = find(id);
        if (i.getStatus() != IncidentStatus.REPORTED) {
            throw new ConflictException("Only REPORTED incidents can be deleted; " + i.getIncidentCode()
                    + " is " + i.getStatus());
        }
        historyRepository.deleteByIncidentId(id);
        assignmentRepository.deleteByIncidentId(id);
        incidentRepository.delete(i);
    }

    // ================================================================ helpers

    Incident find(Long id) {
        return incidentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Incident", id));
    }

    Map<Long, List<AssignedPersonnel>> personnelFor(List<Long> incidentIds) {
        if (incidentIds.isEmpty()) return Map.of();
        return assignmentRepository.findActiveForIncidents(incidentIds).stream()
                .collect(Collectors.groupingBy(a -> a.getIncident().getId(),
                        Collectors.mapping(IncidentMapper::toPersonnel, Collectors.toList())));
    }

    private void writeHistory(Incident i, IncidentStatus from, IncidentStatus to, AuthUser user, String note) {
        IncidentStatusHistory h = new IncidentStatusHistory();
        h.setIncident(i);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setChangedByUserId(user.id());
        h.setChangedByName(user.displayName());
        h.setChangedAt(LocalDateTime.now(clock));
        h.setNote(note);
        historyRepository.save(h);
    }

    /** 404 -> 422; zone in a different area -> 422; habitat unreachable -> warning (CONTRACTS.md 7.6). */
    private String checkZone(Long zoneId, Long areaId, List<String> warnings) {
        try {
            ZoneDto zone = habitatClient.getZone(zoneId);
            if (zone.areaId() != null && !zone.areaId().equals(areaId)) {
                throw new BusinessRuleException("Zone " + zoneId + " belongs to area " + zone.areaId()
                        + ", not area " + areaId);
            }
            return zone.name();
        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            if (RemoteErrors.isNotFound(e)) {
                throw new BusinessRuleException("Zone " + zoneId + " does not exist in habitat-service");
            }
            log.warn("habitat-service unavailable while checking zone {}: {}", zoneId, e.getMessage());
            warnings.add("Zone " + zoneId + " could not be verified with habitat-service; zone name omitted.");
            return null;
        }
    }

    private void requireNotFuture(LocalDateTime occurredAt) {
        if (occurredAt.isAfter(LocalDateTime.now(clock).plusMinutes(10))) {
            throw new BusinessRuleException("occurredAt cannot be in the future");
        }
    }

    private static void apply(Incident i, IncidentRequest req, String zoneName) {
        i.setTitle(req.title().trim());
        i.setIncidentType(req.incidentType());
        i.setSeverity(req.severity());
        i.setDescription(req.description().trim());
        i.setAreaId(req.areaId());
        i.setZoneId(req.zoneId());
        i.setZoneName(zoneName);
        i.setLatitude(req.latitude());
        i.setLongitude(req.longitude());
        i.setLocationDescription(trimToNull(req.locationDescription()));
        i.setOccurredAt(req.occurredAt());
        i.setAnimalId(req.animalId());
        i.setSpeciesId(req.speciesId());
        if (req.sourcePatrolLogId() != null) {
            i.setSourcePatrolLogId(req.sourcePatrolLogId());
        }
    }
}

package com.wildlife.rescue.service;

import com.wildlife.rescue.client.AnimalDto;
import com.wildlife.rescue.client.AnimalStatusUpdate;
import com.wildlife.rescue.client.IncidentClient;
import com.wildlife.rescue.client.RemoteErrors;
import com.wildlife.rescue.client.WildlifeClient;
import com.wildlife.rescue.config.AuthUser;
import com.wildlife.rescue.config.CurrentUser;
import com.wildlife.rescue.dto.PageResponse;
import com.wildlife.rescue.dto.RehabProgressRequest;
import com.wildlife.rescue.dto.RehabProgressResponse;
import com.wildlife.rescue.dto.ReleaseInfo;
import com.wildlife.rescue.dto.RescueDetailResponse;
import com.wildlife.rescue.dto.RescueHistoryResponse;
import com.wildlife.rescue.dto.RescueRequest;
import com.wildlife.rescue.dto.RescueResponse;
import com.wildlife.rescue.dto.RescueTransitionRequest;
import com.wildlife.rescue.dto.TransferInfo;
import com.wildlife.rescue.dto.TreatmentRequest;
import com.wildlife.rescue.dto.TreatmentResponse;
import com.wildlife.rescue.entity.RehabProgress;
import com.wildlife.rescue.entity.RescueCase;
import com.wildlife.rescue.entity.RescueCause;
import com.wildlife.rescue.entity.RescueStatus;
import com.wildlife.rescue.entity.RescueStatusHistory;
import com.wildlife.rescue.entity.Treatment;
import com.wildlife.rescue.exception.BusinessRuleException;
import com.wildlife.rescue.exception.ConflictException;
import com.wildlife.rescue.exception.ResourceNotFoundException;
import com.wildlife.rescue.repository.RehabProgressRepository;
import com.wildlife.rescue.repository.RescueHistoryRepository;
import com.wildlife.rescue.repository.RescueRepository;
import com.wildlife.rescue.repository.TreatmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import static com.wildlife.rescue.service.RescueMapper.trimToNull;

/**
 * Rescue cases (CONTRACTS.md 6.2, 7.7). On creation and every move, the linked animal's status is
 * synchronised in wildlife-service (CONTRACTS.md 9.4); failures there only add warnings.
 */
@Service
@Transactional
public class RescueService {

    private static final Logger log = LoggerFactory.getLogger(RescueService.class);

    private final RescueRepository rescueRepository;
    private final TreatmentRepository treatmentRepository;
    private final RehabProgressRepository rehabRepository;
    private final RescueHistoryRepository historyRepository;
    private final WildlifeClient wildlifeClient;
    private final IncidentClient incidentClient;
    private final Clock clock;

    public RescueService(RescueRepository rescueRepository, TreatmentRepository treatmentRepository,
                         RehabProgressRepository rehabRepository, RescueHistoryRepository historyRepository,
                         WildlifeClient wildlifeClient, IncidentClient incidentClient, Clock clock) {
        this.rescueRepository = rescueRepository;
        this.treatmentRepository = treatmentRepository;
        this.rehabRepository = rehabRepository;
        this.historyRepository = historyRepository;
        this.wildlifeClient = wildlifeClient;
        this.incidentClient = incidentClient;
        this.clock = clock;
    }

    // ================================================================ queries

    @Transactional(readOnly = true)
    public PageResponse<RescueResponse> search(RescueStatus status, RescueCause cause, Long areaId, Long zoneId,
                                               LocalDate from, LocalDate to, String q, Pageable pageable) {
        Specification<RescueCase> spec = Specification.where(null);
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (cause != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("cause"), cause));
        if (areaId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("areaId"), areaId));
        if (zoneId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("zoneId"), zoneId));
        if (from != null) {
            LocalDateTime start = from.atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("rescuedAt"), start));
        }
        if (to != null) {
            LocalDateTime end = to.plusDays(1).atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("rescuedAt"), end));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("caseNumber")), like),
                    cb.like(cb.lower(root.get("speciesName")), like),
                    cb.like(cb.lower(root.get("animalTagId")), like),
                    cb.like(cb.lower(root.get("facilityName")), like)));
        }
        return PageResponse.of(rescueRepository.findAll(spec, pageable), r -> RescueMapper.toRescue(r, null));
    }

    @Transactional(readOnly = true)
    public RescueDetailResponse get(Long id) {
        RescueCase r = find(id);
        return RescueDetailResponse.of(RescueMapper.toRescue(r, null), treatments(id), rehabProgress(id), history(id));
    }

    @Transactional(readOnly = true)
    public List<TreatmentResponse> treatments(Long id) {
        find(id);
        return treatmentRepository.findByRescueIdOrderByTreatmentDateAscIdAsc(id).stream()
                .map(RescueMapper::toTreatment).toList();
    }

    @Transactional(readOnly = true)
    public List<RehabProgressResponse> rehabProgress(Long id) {
        find(id);
        return rehabRepository.findByRescueIdOrderByAssessedOnAscIdAsc(id).stream()
                .map(RescueMapper::toRehab).toList();
    }

    @Transactional(readOnly = true)
    public List<RescueHistoryResponse> history(Long id) {
        find(id);
        return historyRepository.findByRescueIdOrderByChangedAtAscIdAsc(id).stream()
                .map(RescueMapper::toHistory).toList();
    }

    // ================================================================ commands

    public RescueResponse create(RescueRequest req) {
        requireNotFuture(req.rescuedAt(), "rescuedAt");
        List<String> warnings = new ArrayList<>();
        String tagId = verifyAnimal(req.animalId(), warnings);
        verifyIncident(req.incidentId(), warnings);

        AuthUser user = CurrentUser.get();
        LocalDateTime now = LocalDateTime.now(clock);
        RescueCase r = new RescueCase();
        r.setCaseNumber(String.format("RSC-%d-%04d", now.getYear(), rescueRepository.nextCaseNumber()));
        r.setStatus(RescueStatus.REGISTERED);
        r.setRegisteredByUserId(user.id());
        r.setRegisteredByName(user.displayName());
        r.setCreatedAt(now);
        r.setUpdatedAt(now);
        r.setAnimalTagId(tagId);
        apply(r, req);
        RescueCase saved = rescueRepository.save(r);
        writeHistory(saved, null, RescueStatus.REGISTERED, user, "Rescue case registered");

        if (saved.getAnimalId() != null) {
            syncAnimal(saved, new AnimalStatusUpdate("IN_RESCUE", saved.getIntakeCondition().name(), null, null,
                    null, null, reference(saved), "Rescued: " + saved.getCause()), warnings);
        }
        return RescueMapper.toRescue(saved, warnings);
    }

    public RescueResponse update(Long id, RescueRequest req) {
        RescueCase r = find(id);
        if (RescueWorkflow.isFinal(r.getStatus())) {
            throw new ConflictException("Rescue case " + r.getCaseNumber() + " is " + r.getStatus() + " and can no longer be edited");
        }
        requireNotFuture(req.rescuedAt(), "rescuedAt");
        List<String> warnings = new ArrayList<>();
        if (req.animalId() != null && !req.animalId().equals(r.getAnimalId())) {
            r.setAnimalTagId(verifyAnimal(req.animalId(), warnings));
        } else if (req.animalId() == null) {
            r.setAnimalTagId(null);
        }
        if (req.incidentId() != null && !req.incidentId().equals(r.getIncidentId())) {
            verifyIncident(req.incidentId(), warnings);
        }
        apply(r, req);
        r.setUpdatedAt(LocalDateTime.now(clock));
        return RescueMapper.toRescue(r, warnings);
    }

    /** Workflow move with the side effects of CONTRACTS.md 6.2 and 9.4. */
    public RescueResponse transition(Long id, RescueTransitionRequest req) {
        RescueCase r = find(id);
        AuthUser user = CurrentUser.get();
        RescueStatus target = req.toStatus();
        Boolean latestFit = rehabRepository.findFirstByRescueIdOrderByAssessedOnDescIdDesc(id)
                .map(RehabProgress::isFitForRelease).orElse(null);
        RescueWorkflow.check(r.getCaseNumber(), r.getStatus(), target, user.roles(),
                treatmentRepository.countByRescueId(id), latestFit, req.transfer() != null, req.release() != null);

        RescueStatus from = r.getStatus();
        List<String> warnings = new ArrayList<>();
        AnimalStatusUpdate sync = null;
        String ref = reference(r);
        switch (target) {
            case UNDER_TREATMENT -> sync = new AnimalStatusUpdate("IN_RESCUE", "UNDER_TREATMENT", null, null,
                    null, null, ref, trimToNull(req.note()));
            case REHABILITATING -> sync = new AnimalStatusUpdate("IN_RESCUE", "RECOVERING", null, null,
                    null, null, ref, trimToNull(req.note()));
            case TRANSFERRED -> {
                TransferInfo t = req.transfer();
                r.setTransferDestination(t.destinationFacility().trim());
                r.setTransferReason(t.reason().trim());
                r.setTransferDate(t.transferDate());
                sync = new AnimalStatusUpdate("TRANSFERRED", null, null, null, null, null, ref,
                        "Transferred to " + t.destinationFacility().trim());
            }
            case RELEASED -> {
                ReleaseInfo rel = req.release();
                if (rel.releaseDate().isAfter(LocalDate.now(clock))) {
                    throw new BusinessRuleException("releaseDate cannot be in the future");
                }
                r.setReleaseDate(rel.releaseDate());
                r.setReleaseAreaId(rel.releaseAreaId());
                r.setReleaseZoneId(rel.releaseZoneId());
                r.setReleaseLatitude(rel.latitude());
                r.setReleaseLongitude(rel.longitude());
                r.setReleaseNotes(trimToNull(rel.notes()));
                sync = new AnimalStatusUpdate("IN_WILD", "HEALTHY", rel.latitude(), rel.longitude(),
                        rel.releaseAreaId(), rel.releaseZoneId(), ref, trimToNull(rel.notes()));
            }
            default -> { }
        }
        r.setStatus(target);
        r.setUpdatedAt(LocalDateTime.now(clock));
        writeHistory(r, from, target, user, trimToNull(req.note()));
        if (r.getAnimalId() != null && sync != null) {
            syncAnimal(r, sync, warnings);
        }
        return RescueMapper.toRescue(r, warnings);
    }

    public TreatmentResponse addTreatment(Long id, TreatmentRequest req) {
        RescueCase r = find(id);
        if (RescueWorkflow.isFinal(r.getStatus())) {
            throw new ConflictException("Rescue case " + r.getCaseNumber() + " is closed (" + r.getStatus() + ")");
        }
        Treatment t = new Treatment();
        t.setRescue(r);
        t.setTreatmentDate(req.treatmentDate());
        t.setDiagnosis(req.diagnosis().trim());
        t.setProcedure(req.procedure().trim());
        t.setMedications(trimToNull(req.medications()));
        t.setVetStaffId(req.vetStaffId());
        t.setVetName(req.vetName().trim());
        t.setNotes(trimToNull(req.notes()));
        t.setCreatedAt(LocalDateTime.now(clock));
        return RescueMapper.toTreatment(treatmentRepository.save(t));
    }

    public RehabProgressResponse addRehabProgress(Long id, RehabProgressRequest req) {
        RescueCase r = find(id);
        if (r.getStatus() != RescueStatus.REHABILITATING && r.getStatus() != RescueStatus.UNDER_TREATMENT) {
            throw new BusinessRuleException("Rehabilitation progress can only be recorded while the case is "
                    + "UNDER_TREATMENT or REHABILITATING (case is " + r.getStatus() + ")");
        }
        RehabProgress p = new RehabProgress();
        p.setRescue(r);
        p.setAssessedOn(req.assessedOn());
        p.setEnclosure(trimToNull(req.enclosure()));
        p.setBehaviourNotes(req.behaviourNotes().trim());
        p.setWeightKg(req.weightKg());
        p.setFitForRelease(req.fitForRelease());
        p.setAssessedBy(req.assessedBy().trim());
        p.setCreatedAt(LocalDateTime.now(clock));
        return RescueMapper.toRehab(rehabRepository.save(p));
    }

    // ================================================================ helpers

    RescueCase find(Long id) {
        return rescueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rescue case", id));
    }

    private static String reference(RescueCase r) {
        return "RESCUE:" + r.getCaseNumber();
    }

    /** 404 -> 422; unreachable -> warning. Returns the animal's tag id when known. */
    private String verifyAnimal(Long animalId, List<String> warnings) {
        if (animalId == null) return null;
        try {
            AnimalDto animal = wildlifeClient.getAnimal(animalId);
            if ("DECEASED".equals(animal.status())) {
                throw new BusinessRuleException("Animal " + animal.tagId() + " is recorded as deceased");
            }
            return animal.tagId();
        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            if (RemoteErrors.isNotFound(e)) {
                throw new BusinessRuleException("Animal " + animalId + " does not exist in wildlife-service");
            }
            log.warn("wildlife-service unavailable while checking animal {}: {}", animalId, e.getMessage());
            warnings.add("Animal " + animalId + " could not be verified with wildlife-service; tag id omitted.");
            return null;
        }
    }

    /** 404 -> 422; unreachable -> warning. */
    private void verifyIncident(Long incidentId, List<String> warnings) {
        if (incidentId == null) return;
        try {
            incidentClient.getIncident(incidentId);
        } catch (Exception e) {
            if (RemoteErrors.isNotFound(e)) {
                throw new BusinessRuleException("Incident " + incidentId + " does not exist in incident-service");
            }
            log.warn("incident-service unavailable while checking incident {}: {}", incidentId, e.getMessage());
            warnings.add("Incident " + incidentId + " could not be verified with incident-service.");
        }
    }

    private void syncAnimal(RescueCase r, AnimalStatusUpdate update, List<String> warnings) {
        try {
            wildlifeClient.updateStatus(r.getAnimalId(), update);
        } catch (Exception e) {
            log.warn("Could not update animal {} in wildlife-service: {}", r.getAnimalId(), e.getMessage());
            warnings.add("Animal " + r.getAnimalId() + " status could not be updated in wildlife-service ("
                    + RemoteErrors.messageOf(e) + "); update it there manually.");
        }
    }

    private void writeHistory(RescueCase r, RescueStatus from, RescueStatus to, AuthUser user, String note) {
        RescueStatusHistory h = new RescueStatusHistory();
        h.setRescue(r);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setChangedByUserId(user.id());
        h.setChangedByName(user.displayName());
        h.setChangedAt(LocalDateTime.now(clock));
        h.setNote(note);
        historyRepository.save(h);
    }

    private void requireNotFuture(LocalDateTime t, String field) {
        if (t.isAfter(LocalDateTime.now(clock).plusMinutes(10))) {
            throw new BusinessRuleException(field + " cannot be in the future");
        }
    }

    private static void apply(RescueCase r, RescueRequest req) {
        r.setAnimalId(req.animalId());
        r.setSpeciesId(req.speciesId());
        r.setSpeciesName(req.speciesName().trim());
        r.setAnimalDescription(trimToNull(req.animalDescription()));
        r.setCause(req.cause());
        r.setIntakeCondition(req.intakeCondition());
        r.setRescuedAt(req.rescuedAt());
        r.setLatitude(req.latitude());
        r.setLongitude(req.longitude());
        r.setAreaId(req.areaId());
        r.setZoneId(req.zoneId());
        r.setIncidentId(req.incidentId());
        r.setFacilityName(req.facilityName().trim());
        r.setNotes(trimToNull(req.notes()));
    }
}

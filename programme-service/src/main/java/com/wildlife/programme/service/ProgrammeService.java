package com.wildlife.programme.service;

import com.wildlife.programme.config.AuthUser;
import com.wildlife.programme.config.CurrentUser;
import com.wildlife.programme.dto.ActivityRequest;
import com.wildlife.programme.dto.ActivityResponse;
import com.wildlife.programme.dto.PageResponse;
import com.wildlife.programme.dto.ProgrammeDetailResponse;
import com.wildlife.programme.dto.ProgrammeRequest;
import com.wildlife.programme.dto.ProgrammeResponse;
import com.wildlife.programme.dto.ProgressUpdateRequest;
import com.wildlife.programme.dto.ProgressUpdateResponse;
import com.wildlife.programme.dto.TargetRequest;
import com.wildlife.programme.dto.TargetResponse;
import com.wildlife.programme.entity.Activity;
import com.wildlife.programme.entity.ActivityStatus;
import com.wildlife.programme.entity.Programme;
import com.wildlife.programme.entity.ProgrammeStatus;
import com.wildlife.programme.entity.ProgrammeType;
import com.wildlife.programme.entity.ProgressUpdate;
import com.wildlife.programme.entity.Target;
import com.wildlife.programme.exception.BusinessRuleException;
import com.wildlife.programme.exception.ConflictException;
import com.wildlife.programme.exception.ResourceNotFoundException;
import com.wildlife.programme.repository.ActivityRepository;
import com.wildlife.programme.repository.ProgrammeRepository;
import com.wildlife.programme.repository.ProgressUpdateRepository;
import com.wildlife.programme.repository.TargetRepository;
import jakarta.persistence.criteria.Join;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Conservation programmes (CONTRACTS.md 6.5, 7.8). */
@Service
@Transactional
public class ProgrammeService {

    /** Programme status moves (CONTRACTS.md 6.5). */
    static final Map<ProgrammeStatus, Set<ProgrammeStatus>> MOVES = Map.of(
            ProgrammeStatus.PLANNED, EnumSet.of(ProgrammeStatus.ACTIVE, ProgrammeStatus.CANCELLED),
            ProgrammeStatus.ACTIVE, EnumSet.of(ProgrammeStatus.ON_HOLD, ProgrammeStatus.COMPLETED, ProgrammeStatus.CANCELLED),
            ProgrammeStatus.ON_HOLD, EnumSet.of(ProgrammeStatus.ACTIVE, ProgrammeStatus.CANCELLED),
            ProgrammeStatus.COMPLETED, EnumSet.noneOf(ProgrammeStatus.class),
            ProgrammeStatus.CANCELLED, EnumSet.noneOf(ProgrammeStatus.class));

    private final ProgrammeRepository programmeRepository;
    private final ActivityRepository activityRepository;
    private final TargetRepository targetRepository;
    private final ProgressUpdateRepository progressRepository;
    private final StaffNames staffNames;
    private final Clock clock;

    public ProgrammeService(ProgrammeRepository programmeRepository, ActivityRepository activityRepository,
                            TargetRepository targetRepository, ProgressUpdateRepository progressRepository,
                            StaffNames staffNames, Clock clock) {
        this.programmeRepository = programmeRepository;
        this.activityRepository = activityRepository;
        this.targetRepository = targetRepository;
        this.progressRepository = progressRepository;
        this.staffNames = staffNames;
        this.clock = clock;
    }

    // ================================================================ programmes

    @Transactional(readOnly = true)
    public PageResponse<ProgrammeResponse> search(ProgrammeStatus status, ProgrammeType type, Long areaId, String q,
                                                  Pageable pageable) {
        Specification<Programme> spec = Specification.where(null);
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (type != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("programmeType"), type));
        if (areaId != null) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                Join<Programme, Long> areas = root.join("areaIds");
                return cb.equal(areas, areaId);
            });
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("code")), like)));
        }
        Page<Programme> page = programmeRepository.findAll(spec, pageable);
        List<Long> ids = page.getContent().stream().map(Programme::getId).toList();
        Map<Long, List<Target>> targets = ids.isEmpty() ? Map.of()
                : targetRepository.findByProgrammeIds(ids).stream()
                .collect(Collectors.groupingBy(t -> t.getProgramme().getId()));
        StaffNames.Lookup names = staffNames.lookup(page.getContent().stream().map(Programme::getLeadStaffId).toList());
        List<String> warnings = names.warning() == null ? List.of() : List.of(names.warning());
        return PageResponse.of(page, p -> toResponse(p, targets.getOrDefault(p.getId(), List.of()), names, warnings));
    }

    @Transactional(readOnly = true)
    public ProgrammeDetailResponse get(Long id) {
        Programme p = find(id);
        List<Activity> activities = activityRepository.findByProgrammeIdOrderByPlannedStartAscIdAsc(id);
        List<Target> targets = targetRepository.findByProgrammeIdOrderByIdAsc(id);
        Set<Long> staffIds = new LinkedHashSet<>();
        staffIds.add(p.getLeadStaffId());
        activities.forEach(a -> staffIds.add(a.getAssignedStaffId()));
        StaffNames.Lookup names = staffNames.lookup(staffIds);
        List<String> warnings = names.warning() == null ? List.of() : List.of(names.warning());

        List<ProgressUpdateResponse> recent = progressRepository
                .findByProgrammeIdOrderByUpdateDateDescIdDesc(id, PageRequest.of(0, 10)).stream()
                .map(ProgrammeService::toProgress).toList();
        return ProgrammeDetailResponse.of(toResponse(p, targets, names, warnings),
                activities.stream().map(a -> toActivity(a, names)).toList(),
                targets.stream().map(ProgrammeService::toTarget).toList(),
                recent);
    }

    public ProgrammeResponse create(ProgrammeRequest req) {
        String code = req.code().trim().toUpperCase(Locale.ROOT);
        if (programmeRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Programme code " + code + " is already in use");
        }
        Programme p = new Programme();
        p.setStatus(ProgrammeStatus.PLANNED);
        apply(p, req, code);
        Programme saved = programmeRepository.save(p);
        return toResponse(saved, List.of(), staffNames.lookup(Arrays.asList(saved.getLeadStaffId())), null);
    }

    public ProgrammeResponse update(Long id, ProgrammeRequest req) {
        Programme p = find(id);
        String code = req.code().trim().toUpperCase(Locale.ROOT);
        if (programmeRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ConflictException("Programme code " + code + " is already in use");
        }
        apply(p, req, code);
        return toResponse(p, targetRepository.findByProgrammeIdOrderByIdAsc(id),
                staffNames.lookup(Arrays.asList(p.getLeadStaffId())), null);
    }

    public ProgrammeResponse changeStatus(Long id, ProgrammeStatus target) {
        Programme p = find(id);
        if (!MOVES.get(p.getStatus()).contains(target)) {
            throw new ConflictException("Cannot move programme " + p.getCode() + " from " + p.getStatus() + " to " + target);
        }
        p.setStatus(target);
        return toResponse(p, targetRepository.findByProgrammeIdOrderByIdAsc(id),
                staffNames.lookup(Arrays.asList(p.getLeadStaffId())), null);
    }

    public void delete(Long id) {
        Programme p = find(id);
        if (p.getStatus() != ProgrammeStatus.PLANNED) {
            throw new ConflictException("Only PLANNED programmes can be deleted; " + p.getCode() + " is " + p.getStatus());
        }
        progressRepository.deleteByProgrammeId(id);
        activityRepository.deleteByProgrammeId(id);
        targetRepository.deleteByProgrammeId(id);
        programmeRepository.delete(p);
    }

    // ================================================================ activities

    @Transactional(readOnly = true)
    public List<ActivityResponse> activities(Long programmeId) {
        find(programmeId);
        List<Activity> list = activityRepository.findByProgrammeIdOrderByPlannedStartAscIdAsc(programmeId);
        StaffNames.Lookup names = staffNames.lookup(list.stream().map(Activity::getAssignedStaffId).toList());
        return list.stream().map(a -> toActivity(a, names)).toList();
    }

    public ActivityResponse addActivity(Long programmeId, ActivityRequest req) {
        Programme p = findOpen(programmeId);
        Activity a = new Activity();
        a.setProgramme(p);
        applyActivity(a, req);
        Activity saved = activityRepository.save(a);
        return toActivity(saved, staffNames.lookup(Arrays.asList(saved.getAssignedStaffId())));
    }

    public ActivityResponse updateActivity(Long programmeId, Long activityId, ActivityRequest req) {
        findOpen(programmeId);
        Activity a = activityRepository.findByIdAndProgrammeId(activityId, programmeId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity " + activityId + " not found in programme " + programmeId));
        applyActivity(a, req);
        return toActivity(a, staffNames.lookup(Arrays.asList(a.getAssignedStaffId())));
    }

    public void deleteActivity(Long programmeId, Long activityId) {
        Activity a = activityRepository.findByIdAndProgrammeId(activityId, programmeId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity " + activityId + " not found in programme " + programmeId));
        activityRepository.delete(a);
    }

    // ================================================================ targets

    @Transactional(readOnly = true)
    public List<TargetResponse> targets(Long programmeId) {
        find(programmeId);
        return targetRepository.findByProgrammeIdOrderByIdAsc(programmeId).stream()
                .map(ProgrammeService::toTarget).toList();
    }

    public TargetResponse addTarget(Long programmeId, TargetRequest req) {
        Programme p = findOpen(programmeId);
        Target t = new Target();
        t.setProgramme(p);
        applyTarget(t, req);
        t.setCurrentValue(req.currentValue() != null ? req.currentValue() : req.baselineValue());
        t.setLastUpdatedAt(LocalDateTime.now(clock));
        return toTarget(targetRepository.save(t));
    }

    public TargetResponse updateTarget(Long programmeId, Long targetId, TargetRequest req) {
        findOpen(programmeId);
        Target t = findTarget(programmeId, targetId);
        applyTarget(t, req);
        if (req.currentValue() != null) {
            t.setCurrentValue(req.currentValue());
        }
        t.setLastUpdatedAt(LocalDateTime.now(clock));
        return toTarget(t);
    }

    public void deleteTarget(Long programmeId, Long targetId) {
        Target t = findTarget(programmeId, targetId);
        if (progressRepository.existsByTargetId(targetId)) {
            throw new ConflictException("Target '" + t.getMetricName() + "' has progress history and cannot be deleted");
        }
        targetRepository.delete(t);
    }

    // ================================================================ progress updates

    @Transactional(readOnly = true)
    public List<ProgressUpdateResponse> progress(Long programmeId) {
        find(programmeId);
        return progressRepository.findByProgrammeIdOrderByUpdateDateDescIdDesc(programmeId).stream()
                .map(ProgrammeService::toProgress).toList();
    }

    /** A progress update with a target moves that target's current value (CONTRACTS.md 7.8). */
    public ProgressUpdateResponse addProgress(Long programmeId, ProgressUpdateRequest req) {
        Programme p = findOpen(programmeId);
        AuthUser user = CurrentUser.get();
        ProgressUpdate u = new ProgressUpdate();
        u.setProgramme(p);
        u.setUpdateDate(req.updateDate());
        u.setNote(req.note().trim());
        u.setReportedByUserId(user.id());
        u.setReportedByName(user.displayName());
        u.setCreatedAt(LocalDateTime.now(clock));
        if (req.targetId() != null) {
            if (req.newValue() == null) {
                throw new BusinessRuleException("newValue is required when targetId is given");
            }
            Target t = findTarget(programmeId, req.targetId());
            u.setTarget(t);
            u.setPreviousValue(t.getCurrentValue());
            u.setNewValue(req.newValue());
            t.setCurrentValue(req.newValue());
            t.setLastUpdatedAt(LocalDateTime.now(clock));
        }
        return toProgress(progressRepository.save(u));
    }

    // ================================================================ mapping and helpers

    Programme find(Long id) {
        return programmeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Programme", id));
    }

    /** Completed and cancelled programmes are read-only. */
    private Programme findOpen(Long id) {
        Programme p = find(id);
        if (p.getStatus() == ProgrammeStatus.COMPLETED || p.getStatus() == ProgrammeStatus.CANCELLED) {
            throw new ConflictException("Programme " + p.getCode() + " is " + p.getStatus() + " and read-only");
        }
        return p;
    }

    private Target findTarget(Long programmeId, Long targetId) {
        return targetRepository.findByIdAndProgrammeId(targetId, programmeId)
                .orElseThrow(() -> new ResourceNotFoundException("Target " + targetId + " not found in programme " + programmeId));
    }

    private ProgrammeResponse toResponse(Programme p, List<Target> targets, StaffNames.Lookup names, List<String> warnings) {
        List<Double> percents = targets.stream().map(ProgrammeService::percentOf).toList();
        List<String> w = new ArrayList<>(warnings == null ? List.of() : warnings);
        if (names.warning() != null && !w.contains(names.warning())) w.add(names.warning());
        return new ProgrammeResponse(p.getId(), p.getCode(), p.getName(), p.getProgrammeType(), p.getStatus(),
                p.getObjective(), p.getDescription(), p.getStartDate(), p.getEndDate(), p.getBudgetInr(),
                p.getLeadStaffId(), names.nameOf(p.getLeadStaffId()),
                List.copyOf(p.getAreaIds()), List.copyOf(p.getSpeciesIds()),
                ProgressCalculator.average(percents),
                activityRepository.countByProgrammeId(p.getId()),
                activityRepository.countByProgrammeIdAndStatus(p.getId(), ActivityStatus.DONE),
                targets.size(), w);
    }

    private ActivityResponse toActivity(Activity a, StaffNames.Lookup names) {
        boolean overdue = a.getStatus() != ActivityStatus.DONE && a.getPlannedEnd().isBefore(LocalDate.now(clock));
        return new ActivityResponse(a.getId(), a.getProgramme().getId(), a.getTitle(), a.getDescription(),
                a.getPlannedStart(), a.getPlannedEnd(), a.getStatus(), a.getAssignedStaffId(),
                names.nameOf(a.getAssignedStaffId()), a.getAreaId(), overdue);
    }

    static TargetResponse toTarget(Target t) {
        return new TargetResponse(t.getId(), t.getProgramme().getId(), t.getMetricName(), t.getUnit(),
                t.getBaselineValue(), t.getTargetValue(), t.getCurrentValue(), t.getDueDate(), percentOf(t),
                t.getLastUpdatedAt());
    }

    static ProgressUpdateResponse toProgress(ProgressUpdate u) {
        Target t = u.getTarget();
        return new ProgressUpdateResponse(u.getId(), u.getProgramme().getId(), t == null ? null : t.getId(),
                t == null ? null : t.getMetricName(), u.getUpdateDate(), u.getPreviousValue(), u.getNewValue(),
                u.getNote(), u.getReportedByUserId(), u.getReportedByName(), u.getCreatedAt());
    }

    static double percentOf(Target t) {
        return ProgressCalculator.targetPercent(t.getBaselineValue(), t.getTargetValue(), t.getCurrentValue());
    }

    private void apply(Programme p, ProgrammeRequest req, String code) {
        if (req.endDate().isBefore(req.startDate())) {
            throw new BusinessRuleException("endDate must be on or after startDate");
        }
        p.setCode(code);
        p.setName(req.name().trim());
        p.setProgrammeType(req.programmeType());
        p.setObjective(req.objective().trim());
        p.setDescription(req.description() == null || req.description().isBlank() ? null : req.description().trim());
        p.setStartDate(req.startDate());
        p.setEndDate(req.endDate());
        p.setBudgetInr(req.budgetInr());
        p.setLeadStaffId(req.leadStaffId());
        p.getAreaIds().clear();
        if (req.areaIds() != null) p.getAreaIds().addAll(req.areaIds());
        p.getSpeciesIds().clear();
        if (req.speciesIds() != null) p.getSpeciesIds().addAll(req.speciesIds());
    }

    private static void applyActivity(Activity a, ActivityRequest req) {
        if (req.plannedEnd().isBefore(req.plannedStart())) {
            throw new BusinessRuleException("plannedEnd must be on or after plannedStart");
        }
        a.setTitle(req.title().trim());
        a.setDescription(req.description() == null || req.description().isBlank() ? null : req.description().trim());
        a.setPlannedStart(req.plannedStart());
        a.setPlannedEnd(req.plannedEnd());
        a.setStatus(req.status());
        a.setAssignedStaffId(req.assignedStaffId());
        a.setAreaId(req.areaId());
    }

    private static void applyTarget(Target t, TargetRequest req) {
        t.setMetricName(req.metricName().trim());
        t.setUnit(req.unit().trim());
        t.setBaselineValue(req.baselineValue());
        t.setTargetValue(req.targetValue());
        t.setDueDate(req.dueDate());
    }
}

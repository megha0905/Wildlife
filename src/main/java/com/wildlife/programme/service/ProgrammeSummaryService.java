package com.wildlife.programme.service;

import com.wildlife.programme.dto.ProgrammeSummary;
import com.wildlife.programme.entity.ActivityStatus;
import com.wildlife.programme.entity.Programme;
import com.wildlife.programme.entity.ProgrammeStatus;
import com.wildlife.programme.entity.ProgrammeType;
import com.wildlife.programme.entity.Target;
import com.wildlife.programme.repository.ActivityRepository;
import com.wildlife.programme.repository.ProgrammeRepository;
import com.wildlife.programme.repository.TargetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProgrammeSummaryService {

    private final ProgrammeRepository programmeRepository;
    private final ActivityRepository activityRepository;
    private final TargetRepository targetRepository;
    private final Clock clock;

    public ProgrammeSummaryService(ProgrammeRepository programmeRepository, ActivityRepository activityRepository,
                                   TargetRepository targetRepository, Clock clock) {
        this.programmeRepository = programmeRepository;
        this.activityRepository = activityRepository;
        this.targetRepository = targetRepository;
        this.clock = clock;
    }

    public ProgrammeSummary summary() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ProgrammeStatus s : ProgrammeStatus.values()) byStatus.put(s.name(), 0L);
        for (Object[] row : programmeRepository.countGroupedByStatus()) byStatus.put(((ProgrammeStatus) row[0]).name(), (Long) row[1]);
        Map<String, Long> byType = new LinkedHashMap<>();
        for (ProgrammeType t : ProgrammeType.values()) byType.put(t.name(), 0L);
        for (Object[] row : programmeRepository.countGroupedByType()) byType.put(((ProgrammeType) row[0]).name(), (Long) row[1]);

        List<Programme> active = programmeRepository.findAll().stream()
                .filter(p -> p.getStatus() == ProgrammeStatus.ACTIVE).toList();
        List<Long> activeIds = active.stream().map(Programme::getId).toList();
        Map<Long, List<Target>> targets = activeIds.isEmpty() ? Map.of()
                : targetRepository.findByProgrammeIds(activeIds).stream()
                .collect(Collectors.groupingBy(t -> t.getProgramme().getId()));
        List<Double> programmePercents = active.stream()
                .map(p -> ProgressCalculator.average(targets.getOrDefault(p.getId(), List.of()).stream()
                        .map(ProgrammeService::percentOf).toList()))
                .toList();
        BigDecimal budget = active.stream().map(Programme::getBudgetInr)
                .filter(b -> b != null).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ProgrammeSummary(byStatus, byType, active.size(), ProgressCalculator.average(programmePercents),
                activityRepository.countByStatusNotAndPlannedEndBefore(ActivityStatus.DONE, LocalDate.now(clock)),
                budget);
    }
}

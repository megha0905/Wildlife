package com.wildlife.patrol.service;

import com.wildlife.patrol.dto.PatrolSummary;
import com.wildlife.patrol.entity.ObservationType;
import com.wildlife.patrol.entity.PatrolStatus;
import com.wildlife.patrol.repository.LogRepository;
import com.wildlife.patrol.repository.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class PatrolSummaryService {

    private final ScheduleRepository scheduleRepository;
    private final LogRepository logRepository;
    private final Clock clock;

    public PatrolSummaryService(ScheduleRepository scheduleRepository, LogRepository logRepository, Clock clock) {
        this.scheduleRepository = scheduleRepository;
        this.logRepository = logRepository;
        this.clock = clock;
    }

    public PatrolSummary summary() {
        LocalDate today = LocalDate.now(clock);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        LocalDateTime from = monthStart.atStartOfDay();
        LocalDateTime to = monthStart.plusMonths(1).atStartOfDay();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (PatrolStatus s : PatrolStatus.values()) byStatus.put(s.name(), 0L);
        for (Object[] row : scheduleRepository.countByStatusBetween(monthStart, monthEnd)) {
            byStatus.put(((PatrolStatus) row[0]).name(), (Long) row[1]);
        }
        Double distance = scheduleRepository.sumDistanceBetween(PatrolStatus.COMPLETED, monthStart, monthEnd);

        return new PatrolSummary(
                scheduleRepository.countByPatrolDate(today),
                byStatus,
                logRepository.countByLoggedAtGreaterThanEqualAndLoggedAtLessThan(from, to),
                logRepository.countByObservationTypeAndLoggedAtGreaterThanEqualAndLoggedAtLessThan(
                        ObservationType.SUSPICIOUS_ACTIVITY, from, to),
                distance == null ? 0 : distance);
    }
}

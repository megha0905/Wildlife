package com.wildlife.incident.service;

import com.wildlife.incident.dto.IncidentSummary;
import com.wildlife.incident.entity.IncidentStatus;
import com.wildlife.incident.entity.IncidentType;
import com.wildlife.incident.entity.Severity;
import com.wildlife.incident.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class IncidentSummaryService {

    private static final Set<IncidentStatus> OPEN =
            EnumSet.of(IncidentStatus.REPORTED, IncidentStatus.VERIFIED, IncidentStatus.IN_PROGRESS);

    private final IncidentRepository incidentRepository;
    private final Clock clock;

    public IncidentSummaryService(IncidentRepository incidentRepository, Clock clock) {
        this.incidentRepository = incidentRepository;
        this.clock = clock;
    }

    /** Counts for one month (default: current). openIncidents and criticalOpen are across all months. */
    public IncidentSummary summary(Integer year, Integer month) {
        YearMonth ym = (year != null && month != null) ? YearMonth.of(year, month) : YearMonth.from(LocalDate.now(clock));
        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.plusMonths(1).atDay(1).atStartOfDay();

        Map<String, Long> byStatus = zeroFilled(IncidentStatus.values(), incidentRepository.countByStatusBetween(from, to));
        Map<String, Long> byType = zeroFilled(IncidentType.values(), incidentRepository.countByTypeBetween(from, to));
        Map<String, Long> bySeverity = zeroFilled(Severity.values(), incidentRepository.countBySeverityBetween(from, to));
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();

        return new IncidentSummary(ym.getYear(), ym.getMonthValue(), total,
                incidentRepository.countByStatusIn(OPEN), byStatus, byType, bySeverity,
                incidentRepository.countBySeverityAndStatusIn(Severity.CRITICAL, OPEN));
    }

    static <E extends Enum<E>> Map<String, Long> zeroFilled(E[] values, List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (E e : values) map.put(e.name(), 0L);
        for (Object[] row : rows) map.put(((Enum<?>) row[0]).name(), (Long) row[1]);
        return map;
    }
}

package com.wildlife.rescue.service;

import com.wildlife.rescue.dto.RescueSummary;
import com.wildlife.rescue.entity.RescueCause;
import com.wildlife.rescue.entity.RescueStatus;
import com.wildlife.rescue.repository.RescueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class RescueSummaryService {

    private final RescueRepository rescueRepository;
    private final Clock clock;

    public RescueSummaryService(RescueRepository rescueRepository, Clock clock) {
        this.rescueRepository = rescueRepository;
        this.clock = clock;
    }

    public RescueSummary summary() {
        LocalDate today = LocalDate.now(clock);
        LocalDate yearStart = today.withDayOfYear(1);
        LocalDate yearEnd = yearStart.plusYears(1).minusDays(1);
        YearMonth month = YearMonth.from(today);

        return new RescueSummary(
                rescueRepository.countByStatusIn(EnumSet.of(RescueStatus.REGISTERED, RescueStatus.UNDER_TREATMENT,
                        RescueStatus.REHABILITATING)),
                zeroFilled(RescueStatus.values(), rescueRepository.countGroupedByStatus()),
                zeroFilled(RescueCause.values(), rescueRepository.countGroupedByCause()),
                rescueRepository.countByStatusAndReleaseDateBetween(RescueStatus.RELEASED, yearStart, yearEnd),
                rescueRepository.countByStatusAndTransferDateBetween(RescueStatus.TRANSFERRED, yearStart, yearEnd),
                rescueRepository.countRescuedBetween(month.atDay(1).atStartOfDay(),
                        month.plusMonths(1).atDay(1).atStartOfDay()));
    }

    static <E extends Enum<E>> Map<String, Long> zeroFilled(E[] values, List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (E e : values) map.put(e.name(), 0L);
        for (Object[] row : rows) map.put(((Enum<?>) row[0]).name(), (Long) row[1]);
        return map;
    }
}

package com.wildlife.wildlife.service;

import com.wildlife.wildlife.dto.WildlifeSummary;
import com.wildlife.wildlife.entity.AnimalStatus;
import com.wildlife.wildlife.entity.ConservationStatus;
import com.wildlife.wildlife.entity.HealthStatus;
import com.wildlife.wildlife.repository.AnimalRepository;
import com.wildlife.wildlife.repository.SpeciesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class WildlifeSummaryService {

    private final AnimalRepository animalRepository;
    private final SpeciesRepository speciesRepository;
    private final Clock clock;

    public WildlifeSummaryService(AnimalRepository animalRepository, SpeciesRepository speciesRepository, Clock clock) {
        this.animalRepository = animalRepository;
        this.speciesRepository = speciesRepository;
        this.clock = clock;
    }

    public WildlifeSummary summary() {
        Map<String, Long> byHealth = zeroFilled(HealthStatus.values(), animalRepository.countGroupedByHealth());
        Map<String, Long> byStatus = zeroFilled(AnimalStatus.values(), animalRepository.countGroupedByStatus());
        Map<String, Long> byConservation = zeroFilled(ConservationStatus.values(),
                animalRepository.countGroupedByConservationStatus());
        long attention = byHealth.get("INJURED") + byHealth.get("SICK") + byHealth.get("UNDER_TREATMENT");
        long notSeen = animalRepository.countNotSeenSince(AnimalStatus.IN_WILD, LocalDateTime.now(clock).minusDays(30));
        return new WildlifeSummary(speciesRepository.count(), animalRepository.count(),
                byHealth, byStatus, byConservation, attention, notSeen);
    }

    static <E extends Enum<E>> Map<String, Long> zeroFilled(E[] values, List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (E e : values) map.put(e.name(), 0L);
        for (Object[] row : rows) map.put(((Enum<?>) row[0]).name(), (Long) row[1]);
        return map;
    }
}

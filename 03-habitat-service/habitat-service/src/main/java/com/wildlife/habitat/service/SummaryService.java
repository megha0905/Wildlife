package com.wildlife.habitat.service;

import com.wildlife.habitat.dto.HabitatSummary;
import com.wildlife.habitat.entity.HabitatCondition;
import com.wildlife.habitat.entity.WaterStatus;
import com.wildlife.habitat.repository.AreaRepository;
import com.wildlife.habitat.repository.HabitatRepository;
import com.wildlife.habitat.repository.WaterResourceRepository;
import com.wildlife.habitat.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class SummaryService {

    private final AreaRepository areaRepository;
    private final ZoneRepository zoneRepository;
    private final HabitatRepository habitatRepository;
    private final WaterResourceRepository waterRepository;

    public SummaryService(AreaRepository areaRepository, ZoneRepository zoneRepository,
                          HabitatRepository habitatRepository, WaterResourceRepository waterRepository) {
        this.areaRepository = areaRepository;
        this.zoneRepository = zoneRepository;
        this.habitatRepository = habitatRepository;
        this.waterRepository = waterRepository;
    }

    public HabitatSummary summary() {
        Map<String, Long> byCondition = new LinkedHashMap<>();
        for (HabitatCondition c : HabitatCondition.values()) byCondition.put(c.name(), 0L);
        for (Object[] row : habitatRepository.countGroupedByCondition()) {
            byCondition.put(((HabitatCondition) row[0]).name(), (Long) row[1]);
        }

        Map<String, Long> byWater = new LinkedHashMap<>();
        for (WaterStatus s : WaterStatus.values()) byWater.put(s.name(), 0L);
        for (Object[] row : waterRepository.countGroupedByStatus()) {
            byWater.put(((WaterStatus) row[0]).name(), (Long) row[1]);
        }
        long attention = byWater.get("LOW") + byWater.get("DRY") + byWater.get("CONTAMINATED");

        Double total = areaRepository.sumTotalAreaSqKm();
        return new HabitatSummary(areaRepository.count(), zoneRepository.count(),
                total == null ? 0 : total, byCondition, byWater, attention);
    }
}

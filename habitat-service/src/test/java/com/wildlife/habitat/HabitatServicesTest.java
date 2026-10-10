package com.wildlife.habitat;

import com.wildlife.habitat.dto.HabitatSummary;
import com.wildlife.habitat.dto.ZoneRequest;
import com.wildlife.habitat.entity.HabitatCondition;
import com.wildlife.habitat.entity.ProtectedArea;
import com.wildlife.habitat.entity.WaterStatus;
import com.wildlife.habitat.entity.ZoneType;
import com.wildlife.habitat.exception.ConflictException;
import com.wildlife.habitat.repository.AreaRepository;
import com.wildlife.habitat.repository.HabitatRepository;
import com.wildlife.habitat.repository.WaterResourceRepository;
import com.wildlife.habitat.repository.ZoneRepository;
import com.wildlife.habitat.service.AreaService;
import com.wildlife.habitat.service.SummaryService;
import com.wildlife.habitat.service.ZoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HabitatServicesTest {

    private AreaRepository areaRepo;
    private ZoneRepository zoneRepo;
    private HabitatRepository habitatRepo;
    private WaterResourceRepository waterRepo;
    private AreaService areaService;
    private ZoneService zoneService;

    @BeforeEach
    void setUp() {
        areaRepo = mock(AreaRepository.class);
        zoneRepo = mock(ZoneRepository.class);
        habitatRepo = mock(HabitatRepository.class);
        waterRepo = mock(WaterResourceRepository.class);
        areaService = new AreaService(areaRepo, zoneRepo);
        zoneService = new ZoneService(zoneRepo, habitatRepo, waterRepo, areaService);
    }

    @Test
    void areaWithZonesCannotBeDeleted() {
        ProtectedArea kanha = new ProtectedArea();
        kanha.setId(1L);
        kanha.setName("Kanha Tiger Reserve");
        when(areaRepo.findById(1L)).thenReturn(Optional.of(kanha));
        when(zoneRepo.countByAreaId(1L)).thenReturn(2L);

        assertThatThrownBy(() -> areaService.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("2 zone(s)");
        verify(areaRepo, never()).delete(any(ProtectedArea.class));
    }

    @Test
    void duplicateZoneCodeIsRejectedCaseInsensitively() {
        when(zoneRepo.existsByCodeIgnoreCase("KNH-MUK")).thenReturn(true);
        ZoneRequest req = new ZoneRequest(1L, "Mukki again", "knh-muk", ZoneType.CORE, 10.0, 22.3, 80.6, null);

        assertThatThrownBy(() -> zoneService.create(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("KNH-MUK");
    }

    @Test
    void summaryListsEveryEnumValueAndCountsWaterNeedingAttention() {
        when(areaRepo.count()).thenReturn(3L);
        when(zoneRepo.count()).thenReturn(6L);
        when(areaRepo.sumTotalAreaSqKm()).thenReturn(3925.0);
        when(habitatRepo.countGroupedByCondition()).thenReturn(List.<Object[]>of(new Object[]{HabitatCondition.GOOD, 4L}));
        when(waterRepo.countGroupedByStatus()).thenReturn(List.of(
                new Object[]{WaterStatus.AVAILABLE, 4L},
                new Object[]{WaterStatus.LOW, 1L},
                new Object[]{WaterStatus.DRY, 1L},
                new Object[]{WaterStatus.CONTAMINATED, 1L}));

        HabitatSummary s = new SummaryService(areaRepo, zoneRepo, habitatRepo, waterRepo).summary();

        assertThat(s.habitatsByCondition()).containsOnlyKeys("GOOD", "FAIR", "DEGRADED", "CRITICAL");
        assertThat(s.habitatsByCondition().get("CRITICAL")).isZero();
        assertThat(s.waterSourcesNeedingAttention()).isEqualTo(3);
        assertThat(s.totalAreaSqKm()).isEqualTo(3925.0);
    }
}

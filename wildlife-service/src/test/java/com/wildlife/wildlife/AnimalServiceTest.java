package com.wildlife.wildlife;

import com.wildlife.wildlife.dto.AnimalResponse;
import com.wildlife.wildlife.dto.HealthRecordRequest;
import com.wildlife.wildlife.dto.SightingUpdate;
import com.wildlife.wildlife.entity.Animal;
import com.wildlife.wildlife.entity.AnimalStatus;
import com.wildlife.wildlife.entity.HealthRecord;
import com.wildlife.wildlife.entity.HealthRecordType;
import com.wildlife.wildlife.entity.HealthStatus;
import com.wildlife.wildlife.entity.Sex;
import com.wildlife.wildlife.entity.Species;
import com.wildlife.wildlife.exception.BusinessRuleException;
import com.wildlife.wildlife.repository.AnimalRepository;
import com.wildlife.wildlife.repository.HealthRecordRepository;
import com.wildlife.wildlife.service.AnimalService;
import com.wildlife.wildlife.service.SpeciesService;
import com.wildlife.wildlife.service.ZoneLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnimalServiceTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-10T06:00:00Z"), IST);

    private AnimalRepository animals;
    private HealthRecordRepository records;
    private AnimalService service;
    private Animal tiger;

    @BeforeEach
    void setUp() {
        animals = mock(AnimalRepository.class);
        records = mock(HealthRecordRepository.class);
        service = new AnimalService(animals, records, mock(SpeciesService.class), mock(ZoneLookup.class), clock);

        Species s = new Species();
        s.setId(1L);
        s.setCommonName("Bengal Tiger");
        tiger = new Animal();
        tiger.setId(1L);
        tiger.setTagId("TGR-KNH-001");
        tiger.setSpecies(s);
        tiger.setSex(Sex.MALE);
        tiger.setStatus(AnimalStatus.IN_WILD);
        tiger.setHealthStatus(HealthStatus.HEALTHY);
        tiger.setLastSeenAt(LocalDateTime.of(2026, 10, 8, 9, 0));
        when(animals.findWithSpeciesById(1L)).thenReturn(Optional.of(tiger));
        when(records.save(any(HealthRecord.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void newestHealthRecordUpdatesCurrentHealth() {
        when(records.findLatestRecordDate(1L)).thenReturn(Optional.of(LocalDate.of(2026, 9, 1)));

        service.addHealthRecord(1L, record(LocalDate.of(2026, 10, 9), HealthStatus.INJURED));

        assertThat(tiger.getHealthStatus()).isEqualTo(HealthStatus.INJURED);
    }

    @Test
    void backFilledOlderRecordDoesNotOverwriteCurrentHealth() {
        when(records.findLatestRecordDate(1L)).thenReturn(Optional.of(LocalDate.of(2026, 10, 9)));

        service.addHealthRecord(1L, record(LocalDate.of(2026, 5, 1), HealthStatus.SICK));

        assertThat(tiger.getHealthStatus()).isEqualTo(HealthStatus.HEALTHY);
    }

    @Test
    void deceasedRecordAlsoSetsStatusDeceased() {
        when(records.findLatestRecordDate(1L)).thenReturn(Optional.empty());

        service.addHealthRecord(1L, record(LocalDate.of(2026, 10, 10), HealthStatus.DECEASED));

        assertThat(tiger.getStatus()).isEqualTo(AnimalStatus.DECEASED);
    }

    @Test
    void olderSightingDoesNotMoveLastSeenBackwards() {
        AnimalResponse r = service.recordSighting(1L, new SightingUpdate(
                LocalDateTime.of(2026, 10, 1, 7, 0), 22.1, 80.1, null, null, null, "PATROL:1", null));

        assertThat(r.lastSeenAt()).isEqualTo(LocalDateTime.of(2026, 10, 8, 9, 0));
    }

    @Test
    void newerSightingMovesLocationAndFindsMissingAnimal() {
        tiger.setStatus(AnimalStatus.MISSING);

        service.recordSighting(1L, new SightingUpdate(
                LocalDateTime.of(2026, 10, 10, 10, 0), 22.5, 80.5, null, null, null, "PATROL:1", null));

        assertThat(tiger.getLastSeenLatitude()).isEqualTo(22.5);
        assertThat(tiger.getStatus()).isEqualTo(AnimalStatus.IN_WILD);
    }

    @Test
    void futureSightingIsRejected() {
        assertThatThrownBy(() -> service.recordSighting(1L, new SightingUpdate(
                LocalDateTime.of(2026, 10, 11, 10, 0), 22.5, 80.5, null, null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    private static HealthRecordRequest record(LocalDate date, HealthStatus status) {
        return new HealthRecordRequest(date, HealthRecordType.OBSERVATION, status, null, null, null, null, null);
    }
}

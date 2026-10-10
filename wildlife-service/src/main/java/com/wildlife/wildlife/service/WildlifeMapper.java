package com.wildlife.wildlife.service;

import com.wildlife.wildlife.dto.AnimalResponse;
import com.wildlife.wildlife.dto.HealthRecordResponse;
import com.wildlife.wildlife.dto.SpeciesResponse;
import com.wildlife.wildlife.entity.Animal;
import com.wildlife.wildlife.entity.HealthRecord;
import com.wildlife.wildlife.entity.Species;

import java.time.Year;
import java.util.List;

final class WildlifeMapper {

    private WildlifeMapper() {
    }

    static SpeciesResponse toSpecies(Species s, long animalCount) {
        return new SpeciesResponse(s.getId(), s.getCommonName(), s.getScientificName(), s.getCategory(),
                s.getConservationStatus(), s.getProtectionSchedule(), s.getDescription(), animalCount);
    }

    static AnimalResponse toAnimalWithWarnings(Animal a, List<String> warnings) {
        Species s = a.getSpecies();
        Integer age = a.getBirthYear() == null ? null : Math.max(0, Year.now().getValue() - a.getBirthYear());
        return new AnimalResponse(a.getId(), a.getTagId(), a.getName(), s.getId(), s.getCommonName(),
                s.getScientificName(), s.getConservationStatus(), a.getSex(), a.getBirthYear(),
                a.isBirthYearEstimated(), age, a.getHealthStatus(), a.getStatus(), a.getIdentificationMarks(),
                a.getAreaId(), a.getZoneId(), a.getZoneName(), a.getLastSeenLatitude(), a.getLastSeenLongitude(),
                a.getLastSeenAt(), a.getNotes(), a.getCreatedAt(), a.getUpdatedAt(),
                warnings == null ? List.of() : warnings);
    }

    static AnimalResponse toAnimal(Animal a) {
        return toAnimalWithWarnings(a, List.of());
    }

    static HealthRecordResponse toRecord(HealthRecord r) {
        return new HealthRecordResponse(r.getId(), r.getAnimal().getId(), r.getRecordDate(), r.getRecordType(),
                r.getHealthStatus(), r.getWeightKg(), r.getDiagnosis(), r.getTreatment(), r.getVetName(),
                r.getRecordedByUserId(), r.getRecordedByName(), r.getNotes(), r.getCreatedAt());
    }

    static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

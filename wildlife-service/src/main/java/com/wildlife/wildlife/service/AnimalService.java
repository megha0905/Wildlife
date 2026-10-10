package com.wildlife.wildlife.service;

import com.wildlife.wildlife.config.AuthUser;
import com.wildlife.wildlife.config.CurrentUser;
import com.wildlife.wildlife.dto.AnimalRequest;
import com.wildlife.wildlife.dto.AnimalResponse;
import com.wildlife.wildlife.dto.AnimalStatusUpdate;
import com.wildlife.wildlife.dto.HealthRecordRequest;
import com.wildlife.wildlife.dto.HealthRecordResponse;
import com.wildlife.wildlife.dto.PageResponse;
import com.wildlife.wildlife.dto.SightingUpdate;
import com.wildlife.wildlife.entity.Animal;
import com.wildlife.wildlife.entity.AnimalStatus;
import com.wildlife.wildlife.entity.HealthRecord;
import com.wildlife.wildlife.entity.HealthStatus;
import com.wildlife.wildlife.exception.BusinessRuleException;
import com.wildlife.wildlife.exception.ConflictException;
import com.wildlife.wildlife.exception.ResourceNotFoundException;
import com.wildlife.wildlife.repository.AnimalRepository;
import com.wildlife.wildlife.repository.HealthRecordRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.wildlife.wildlife.service.WildlifeMapper.trimToNull;

@Service
@Transactional
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final HealthRecordRepository recordRepository;
    private final SpeciesService speciesService;
    private final ZoneLookup zoneLookup;
    private final Clock clock;

    public AnimalService(AnimalRepository animalRepository, HealthRecordRepository recordRepository,
                         SpeciesService speciesService, ZoneLookup zoneLookup, Clock clock) {
        this.animalRepository = animalRepository;
        this.recordRepository = recordRepository;
        this.speciesService = speciesService;
        this.zoneLookup = zoneLookup;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public PageResponse<AnimalResponse> search(String q, Long speciesId, HealthStatus health, AnimalStatus status,
                                               Long areaId, Long zoneId, Pageable pageable) {
        Specification<Animal> spec = Specification.where(null);
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("tagId")), like),
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("species").get("commonName")), like)));
        }
        if (speciesId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("species").get("id"), speciesId));
        if (health != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("healthStatus"), health));
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (areaId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("areaId"), areaId));
        if (zoneId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("zoneId"), zoneId));
        return PageResponse.of(animalRepository.findAll(spec, pageable), WildlifeMapper::toAnimal);
    }

    @Transactional(readOnly = true)
    public AnimalResponse get(Long id) {
        return WildlifeMapper.toAnimal(find(id));
    }

    @Transactional(readOnly = true)
    public AnimalResponse getByTag(String tagId) {
        return animalRepository.findByTagIdIgnoreCase(tagId.trim())
                .map(WildlifeMapper::toAnimal)
                .orElseThrow(() -> new ResourceNotFoundException("No animal with tag " + tagId));
    }

    // ------------------------------------------------------------------ commands

    public AnimalResponse create(AnimalRequest req) {
        String tag = req.tagId().trim().toUpperCase(Locale.ROOT);
        if (animalRepository.existsByTagIdIgnoreCase(tag)) {
            throw new ConflictException("Tag " + tag + " is already assigned to another animal");
        }
        Animal a = new Animal();
        LocalDateTime now = LocalDateTime.now(clock);
        a.setCreatedAt(now);
        List<String> warnings = apply(a, req, tag);
        a.setUpdatedAt(now);
        return WildlifeMapper.toAnimalWithWarnings(animalRepository.save(a), warnings);
    }

    public AnimalResponse update(Long id, AnimalRequest req) {
        Animal a = find(id);
        String tag = req.tagId().trim().toUpperCase(Locale.ROOT);
        if (animalRepository.existsByTagIdIgnoreCaseAndIdNot(tag, id)) {
            throw new ConflictException("Tag " + tag + " is already assigned to another animal");
        }
        List<String> warnings = apply(a, req, tag);
        a.setUpdatedAt(LocalDateTime.now(clock));
        return WildlifeMapper.toAnimalWithWarnings(a, warnings);
    }

    /** Used by rescue-service on case transitions and by officers in the UI. */
    public AnimalResponse updateStatus(Long id, AnimalStatusUpdate req) {
        Animal a = find(id);
        List<String> warnings = new ArrayList<>();
        a.setStatus(req.status());
        if (req.status() == AnimalStatus.DECEASED) {
            a.setHealthStatus(HealthStatus.DECEASED);
        } else if (req.healthStatus() != null) {
            a.setHealthStatus(req.healthStatus());
        }
        // Release back into the wild: move the last-seen location to the release point
        if (req.status() == AnimalStatus.IN_WILD && req.latitude() != null && req.longitude() != null) {
            a.setLastSeenLatitude(req.latitude());
            a.setLastSeenLongitude(req.longitude());
            a.setLastSeenAt(LocalDateTime.now(clock));
        }
        if (req.zoneId() != null) {
            ZoneLookup.ZoneCheck zc = zoneLookup.check(req.zoneId(), req.areaId());
            a.setZoneId(req.zoneId());
            a.setAreaId(zc.areaId());
            a.setZoneName(zc.zoneName());
            if (zc.warning() != null) warnings.add(zc.warning());
        }
        appendNote(a, req.sourceReference(), "Status set to " + req.status(), req.note());
        a.setUpdatedAt(LocalDateTime.now(clock));
        return WildlifeMapper.toAnimalWithWarnings(a, warnings);
    }

    /** Used by patrol-service when a patrol log mentions this animal's tag. */
    public AnimalResponse recordSighting(Long id, SightingUpdate req) {
        Animal a = find(id);
        if (a.getStatus() == AnimalStatus.DECEASED) {
            throw new BusinessRuleException("Animal " + a.getTagId() + " is recorded as deceased");
        }
        if (req.seenAt().isAfter(LocalDateTime.now(clock).plusMinutes(10))) {
            throw new BusinessRuleException("Sighting time cannot be in the future");
        }
        List<String> warnings = new ArrayList<>();
        boolean newer = a.getLastSeenAt() == null || req.seenAt().isAfter(a.getLastSeenAt());
        if (newer) {
            a.setLastSeenAt(req.seenAt());
            a.setLastSeenLatitude(req.latitude());
            a.setLastSeenLongitude(req.longitude());
            if (req.zoneId() != null) {
                ZoneLookup.ZoneCheck zc = zoneLookup.check(req.zoneId(), req.areaId());
                a.setZoneId(req.zoneId());
                a.setAreaId(zc.areaId());
                a.setZoneName(zc.zoneName());
                if (zc.warning() != null) warnings.add(zc.warning());
            }
            if (req.healthStatus() != null) {
                a.setHealthStatus(req.healthStatus());
            }
            if (a.getStatus() == AnimalStatus.MISSING) {
                a.setStatus(AnimalStatus.IN_WILD);
            }
            a.setUpdatedAt(LocalDateTime.now(clock));
        }
        return WildlifeMapper.toAnimalWithWarnings(a, warnings);
    }

    public void delete(Long id) {
        Animal a = find(id);
        recordRepository.deleteByAnimalId(id);
        animalRepository.delete(a);
    }

    // ------------------------------------------------------------------ health records

    @Transactional(readOnly = true)
    public List<HealthRecordResponse> healthRecords(Long animalId) {
        find(animalId);
        return recordRepository.findByAnimalIdOrderByRecordDateDescIdDesc(animalId).stream()
                .map(WildlifeMapper::toRecord).toList();
    }

    /**
     * Adds a health record. The animal's current health changes only if this record is the newest
     * by date (CONTRACTS.md 7.3), so back-filling old paperwork never overwrites today's status.
     */
    public HealthRecordResponse addHealthRecord(Long animalId, HealthRecordRequest req) {
        Animal a = find(animalId);
        LocalDate latest = recordRepository.findLatestRecordDate(animalId).orElse(null);
        AuthUser user = CurrentUser.get();

        HealthRecord r = new HealthRecord();
        r.setAnimal(a);
        r.setRecordDate(req.recordDate());
        r.setRecordType(req.recordType());
        r.setHealthStatus(req.healthStatus());
        r.setWeightKg(req.weightKg());
        r.setDiagnosis(trimToNull(req.diagnosis()));
        r.setTreatment(trimToNull(req.treatment()));
        r.setVetName(trimToNull(req.vetName()));
        r.setRecordedByUserId(user.id());
        r.setRecordedByName(user.displayName());
        r.setNotes(trimToNull(req.notes()));
        r.setCreatedAt(LocalDateTime.now(clock));
        HealthRecord saved = recordRepository.save(r);

        if (latest == null || !req.recordDate().isBefore(latest)) {
            a.setHealthStatus(req.healthStatus());
            if (req.healthStatus() == HealthStatus.DECEASED) {
                a.setStatus(AnimalStatus.DECEASED);
            }
            a.setUpdatedAt(LocalDateTime.now(clock));
        }
        return WildlifeMapper.toRecord(saved);
    }

    // ------------------------------------------------------------------ helpers

    Animal find(Long id) {
        return animalRepository.findWithSpeciesById(id).orElseThrow(() -> new ResourceNotFoundException("Animal", id));
    }

    private List<String> apply(Animal a, AnimalRequest req, String tag) {
        if (req.birthYear() != null && req.birthYear() > Year.now(clock).getValue()) {
            throw new BusinessRuleException("Birth year cannot be in the future");
        }
        if ((req.lastSeenLatitude() == null) != (req.lastSeenLongitude() == null)) {
            throw new BusinessRuleException("Provide both last-seen latitude and longitude, or neither");
        }
        if (req.lastSeenAt() != null && req.lastSeenAt().isAfter(LocalDateTime.now(clock).plusMinutes(10))) {
            throw new BusinessRuleException("Last-seen time cannot be in the future");
        }
        List<String> warnings = new ArrayList<>();
        ZoneLookup.ZoneCheck zc = zoneLookup.check(req.zoneId(), req.areaId());
        if (zc.warning() != null) warnings.add(zc.warning());

        a.setTagId(tag);
        a.setName(trimToNull(req.name()));
        a.setSpecies(speciesService.find(req.speciesId()));
        a.setSex(req.sex());
        a.setBirthYear(req.birthYear());
        a.setBirthYearEstimated(req.birthYearEstimated() == null || req.birthYearEstimated());
        a.setStatus(req.status());
        a.setHealthStatus(req.status() == AnimalStatus.DECEASED ? HealthStatus.DECEASED : req.healthStatus());
        a.setIdentificationMarks(trimToNull(req.identificationMarks()));
        a.setAreaId(zc.areaId());
        a.setZoneId(req.zoneId());
        a.setZoneName(zc.zoneName());
        a.setLastSeenLatitude(req.lastSeenLatitude());
        a.setLastSeenLongitude(req.lastSeenLongitude());
        a.setLastSeenAt(req.lastSeenAt());
        a.setNotes(trimToNull(req.notes()));
        return warnings;
    }

    private void appendNote(Animal a, String sourceReference, String what, String note) {
        StringBuilder line = new StringBuilder("[").append(LocalDate.now(clock)).append("] ").append(what);
        if (trimToNull(sourceReference) != null) line.append(" (").append(sourceReference.trim()).append(")");
        if (trimToNull(note) != null) line.append(": ").append(note.trim());
        String existing = a.getNotes() == null ? "" : a.getNotes() + "\n";
        String combined = existing + line;
        // keep the most recent part if notes grow beyond the column size
        a.setNotes(combined.length() > 2000 ? combined.substring(combined.length() - 2000) : combined);
    }
}

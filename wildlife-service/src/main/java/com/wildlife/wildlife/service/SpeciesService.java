package com.wildlife.wildlife.service;

import com.wildlife.wildlife.dto.PageResponse;
import com.wildlife.wildlife.dto.SpeciesRequest;
import com.wildlife.wildlife.dto.SpeciesResponse;
import com.wildlife.wildlife.entity.ConservationStatus;
import com.wildlife.wildlife.entity.Species;
import com.wildlife.wildlife.entity.SpeciesCategory;
import com.wildlife.wildlife.exception.ConflictException;
import com.wildlife.wildlife.exception.ResourceNotFoundException;
import com.wildlife.wildlife.repository.AnimalRepository;
import com.wildlife.wildlife.repository.SpeciesRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.wildlife.wildlife.service.WildlifeMapper.trimToNull;

@Service
@Transactional
public class SpeciesService {

    private final SpeciesRepository speciesRepository;
    private final AnimalRepository animalRepository;

    public SpeciesService(SpeciesRepository speciesRepository, AnimalRepository animalRepository) {
        this.speciesRepository = speciesRepository;
        this.animalRepository = animalRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<SpeciesResponse> search(String q, SpeciesCategory category, ConservationStatus status,
                                                Pageable pageable) {
        Specification<Species> spec = Specification.where(null);
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("commonName")), like),
                    cb.like(cb.lower(root.get("scientificName")), like)));
        }
        if (category != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("conservationStatus"), status));
        }
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : animalRepository.countGroupedBySpecies()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return PageResponse.of(speciesRepository.findAll(spec, pageable),
                s -> WildlifeMapper.toSpecies(s, counts.getOrDefault(s.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public SpeciesResponse get(Long id) {
        return WildlifeMapper.toSpecies(find(id), animalRepository.countBySpeciesId(id));
    }

    public SpeciesResponse create(SpeciesRequest req) {
        if (speciesRepository.existsByCommonNameIgnoreCase(req.commonName().trim())) {
            throw new ConflictException("Species '" + req.commonName().trim() + "' already exists");
        }
        if (speciesRepository.existsByScientificNameIgnoreCase(req.scientificName().trim())) {
            throw new ConflictException("Scientific name '" + req.scientificName().trim() + "' is already registered");
        }
        Species s = new Species();
        apply(s, req);
        return WildlifeMapper.toSpecies(speciesRepository.save(s), 0);
    }

    public SpeciesResponse update(Long id, SpeciesRequest req) {
        Species s = find(id);
        if (speciesRepository.existsByCommonNameIgnoreCaseAndIdNot(req.commonName().trim(), id)) {
            throw new ConflictException("Species '" + req.commonName().trim() + "' already exists");
        }
        if (speciesRepository.existsByScientificNameIgnoreCaseAndIdNot(req.scientificName().trim(), id)) {
            throw new ConflictException("Scientific name '" + req.scientificName().trim() + "' is already registered");
        }
        apply(s, req);
        return WildlifeMapper.toSpecies(s, animalRepository.countBySpeciesId(id));
    }

    public void delete(Long id) {
        Species s = find(id);
        long animals = animalRepository.countBySpeciesId(id);
        if (animals > 0) {
            throw new ConflictException("Cannot delete " + s.getCommonName() + ": " + animals + " animal(s) use it");
        }
        speciesRepository.delete(s);
    }

    Species find(Long id) {
        return speciesRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Species", id));
    }

    private static void apply(Species s, SpeciesRequest req) {
        s.setCommonName(req.commonName().trim());
        s.setScientificName(req.scientificName().trim());
        s.setCategory(req.category());
        s.setConservationStatus(req.conservationStatus());
        s.setProtectionSchedule(trimToNull(req.protectionSchedule()));
        s.setDescription(trimToNull(req.description()));
    }
}

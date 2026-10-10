package com.wildlife.habitat.service;

import com.wildlife.habitat.dto.HabitatRequest;
import com.wildlife.habitat.dto.HabitatResponse;
import com.wildlife.habitat.dto.PageResponse;
import com.wildlife.habitat.entity.Habitat;
import com.wildlife.habitat.entity.HabitatCondition;
import com.wildlife.habitat.entity.HabitatType;
import com.wildlife.habitat.exception.ResourceNotFoundException;
import com.wildlife.habitat.repository.HabitatRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.wildlife.habitat.service.HabitatMapper.trimToNull;

@Service
@Transactional
public class HabitatService {

    private final HabitatRepository habitatRepository;
    private final ZoneService zoneService;

    public HabitatService(HabitatRepository habitatRepository, ZoneService zoneService) {
        this.habitatRepository = habitatRepository;
        this.zoneService = zoneService;
    }

    @Transactional(readOnly = true)
    public PageResponse<HabitatResponse> search(Long zoneId, Long areaId, HabitatType type,
                                                HabitatCondition condition, Pageable pageable) {
        Specification<Habitat> spec = Specification.where(null);
        if (zoneId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("zone").get("id"), zoneId));
        }
        if (areaId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("zone").get("area").get("id"), areaId));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("habitatType"), type));
        }
        if (condition != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("condition"), condition));
        }
        return PageResponse.of(habitatRepository.findAll(spec, pageable), HabitatMapper::toHabitat);
    }

    @Transactional(readOnly = true)
    public HabitatResponse get(Long id) {
        return HabitatMapper.toHabitat(find(id));
    }

    public HabitatResponse create(HabitatRequest req) {
        Habitat h = new Habitat();
        apply(h, req);
        return HabitatMapper.toHabitat(habitatRepository.save(h));
    }

    public HabitatResponse update(Long id, HabitatRequest req) {
        Habitat h = find(id);
        apply(h, req);
        return HabitatMapper.toHabitat(h);
    }

    public void delete(Long id) {
        habitatRepository.delete(find(id));
    }

    private Habitat find(Long id) {
        return habitatRepository.findWithZoneById(id).orElseThrow(() -> new ResourceNotFoundException("Habitat", id));
    }

    private void apply(Habitat h, HabitatRequest req) {
        h.setZone(zoneService.find(req.zoneId()));
        h.setName(req.name().trim());
        h.setHabitatType(req.habitatType());
        h.setCondition(req.condition());
        h.setAreaSqKm(req.areaSqKm());
        h.setLatitude(req.latitude());
        h.setLongitude(req.longitude());
        h.setDominantVegetation(trimToNull(req.dominantVegetation()));
        h.setLastSurveyDate(req.lastSurveyDate());
        h.setNotes(trimToNull(req.notes()));
    }
}

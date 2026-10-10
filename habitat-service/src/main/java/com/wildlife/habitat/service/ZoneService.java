package com.wildlife.habitat.service;

import com.wildlife.habitat.dto.PageResponse;
import com.wildlife.habitat.dto.ZoneRequest;
import com.wildlife.habitat.dto.ZoneResponse;
import com.wildlife.habitat.entity.Zone;
import com.wildlife.habitat.entity.ZoneType;
import com.wildlife.habitat.exception.ConflictException;
import com.wildlife.habitat.exception.ResourceNotFoundException;
import com.wildlife.habitat.repository.HabitatRepository;
import com.wildlife.habitat.repository.WaterResourceRepository;
import com.wildlife.habitat.repository.ZoneRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

import static com.wildlife.habitat.service.HabitatMapper.trimToNull;

@Service
@Transactional
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final HabitatRepository habitatRepository;
    private final WaterResourceRepository waterRepository;
    private final AreaService areaService;

    public ZoneService(ZoneRepository zoneRepository, HabitatRepository habitatRepository,
                       WaterResourceRepository waterRepository, AreaService areaService) {
        this.zoneRepository = zoneRepository;
        this.habitatRepository = habitatRepository;
        this.waterRepository = waterRepository;
        this.areaService = areaService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ZoneResponse> search(Long areaId, ZoneType type, String q, Pageable pageable) {
        Specification<Zone> spec = Specification.where(null);
        if (areaId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("area").get("id"), areaId));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("zoneType"), type));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("code")), like)));
        }
        return PageResponse.of(zoneRepository.findAll(spec, pageable), HabitatMapper::toZone);
    }

    @Transactional(readOnly = true)
    public ZoneResponse get(Long id) {
        return HabitatMapper.toZone(find(id));
    }

    public ZoneResponse create(ZoneRequest req) {
        String code = req.code().trim().toUpperCase(Locale.ROOT);
        if (zoneRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("A zone with code " + code + " already exists");
        }
        Zone zone = new Zone();
        apply(zone, req, code);
        return HabitatMapper.toZone(zoneRepository.save(zone));
    }

    public ZoneResponse update(Long id, ZoneRequest req) {
        Zone zone = find(id);
        String code = req.code().trim().toUpperCase(Locale.ROOT);
        if (zoneRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ConflictException("A zone with code " + code + " already exists");
        }
        apply(zone, req, code);
        return HabitatMapper.toZone(zone);
    }

    public void delete(Long id) {
        Zone zone = find(id);
        long habitats = habitatRepository.countByZoneId(id);
        long water = waterRepository.countByZoneId(id);
        if (habitats + water > 0) {
            throw new ConflictException("Cannot delete zone " + zone.getName() + ": it still has "
                    + habitats + " habitat(s) and " + water + " water resource(s)");
        }
        zoneRepository.delete(zone);
    }

    Zone find(Long id) {
        return zoneRepository.findWithAreaById(id).orElseThrow(() -> new ResourceNotFoundException("Zone", id));
    }

    private void apply(Zone z, ZoneRequest req, String code) {
        z.setArea(areaService.find(req.areaId()));
        z.setName(req.name().trim());
        z.setCode(code);
        z.setZoneType(req.zoneType());
        z.setAreaSqKm(req.areaSqKm());
        z.setCenterLatitude(req.centerLatitude());
        z.setCenterLongitude(req.centerLongitude());
        z.setDescription(trimToNull(req.description()));
    }
}

package com.wildlife.habitat.service;

import com.wildlife.habitat.dto.PageResponse;
import com.wildlife.habitat.dto.WaterResourceRequest;
import com.wildlife.habitat.dto.WaterResourceResponse;
import com.wildlife.habitat.dto.WaterStatusUpdate;
import com.wildlife.habitat.entity.WaterResource;
import com.wildlife.habitat.entity.WaterSourceType;
import com.wildlife.habitat.entity.WaterStatus;
import com.wildlife.habitat.exception.BusinessRuleException;
import com.wildlife.habitat.exception.ResourceNotFoundException;
import com.wildlife.habitat.repository.WaterResourceRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

import static com.wildlife.habitat.service.HabitatMapper.trimToNull;

@Service
@Transactional
public class WaterResourceService {

    private final WaterResourceRepository waterRepository;
    private final ZoneService zoneService;
    private final Clock clock;

    public WaterResourceService(WaterResourceRepository waterRepository, ZoneService zoneService, Clock clock) {
        this.waterRepository = waterRepository;
        this.zoneService = zoneService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<WaterResourceResponse> search(Long zoneId, Long areaId, WaterSourceType type,
                                                      WaterStatus status, Pageable pageable) {
        Specification<WaterResource> spec = Specification.where(null);
        if (zoneId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("zone").get("id"), zoneId));
        }
        if (areaId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("zone").get("area").get("id"), areaId));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("sourceType"), type));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return PageResponse.of(waterRepository.findAll(spec, pageable), HabitatMapper::toWater);
    }

    @Transactional(readOnly = true)
    public WaterResourceResponse get(Long id) {
        return HabitatMapper.toWater(find(id));
    }

    public WaterResourceResponse create(WaterResourceRequest req) {
        WaterResource w = new WaterResource();
        apply(w, req);
        return HabitatMapper.toWater(waterRepository.save(w));
    }

    public WaterResourceResponse update(Long id, WaterResourceRequest req) {
        WaterResource w = find(id);
        apply(w, req);
        return HabitatMapper.toWater(w);
    }

    /** Field update by rangers after an inspection. */
    public WaterResourceResponse updateStatus(Long id, WaterStatusUpdate req) {
        WaterResource w = find(id);
        LocalDateTime inspectedAt = req.inspectedAt() != null ? req.inspectedAt() : LocalDateTime.now(clock);
        if (inspectedAt.isAfter(LocalDateTime.now(clock).plusMinutes(10))) {
            throw new BusinessRuleException("Inspection time cannot be in the future");
        }
        w.setStatus(req.status());
        w.setLastInspectedAt(inspectedAt);
        if (trimToNull(req.notes()) != null) {
            w.setNotes(req.notes().trim());
        }
        return HabitatMapper.toWater(w);
    }

    public void delete(Long id) {
        waterRepository.delete(find(id));
    }

    private WaterResource find(Long id) {
        return waterRepository.findWithZoneById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Water resource", id));
    }

    private void apply(WaterResource w, WaterResourceRequest req) {
        w.setZone(zoneService.find(req.zoneId()));
        w.setName(req.name().trim());
        w.setSourceType(req.sourceType());
        w.setStatus(req.status());
        w.setPerennial(req.perennial());
        w.setLatitude(req.latitude());
        w.setLongitude(req.longitude());
        w.setCapacityCubicM(req.capacityCubicM());
        w.setLastInspectedAt(req.lastInspectedAt());
        w.setNotes(trimToNull(req.notes()));
    }
}

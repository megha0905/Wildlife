package com.wildlife.habitat.service;

import com.wildlife.habitat.dto.AreaDetailResponse;
import com.wildlife.habitat.dto.AreaRequest;
import com.wildlife.habitat.dto.AreaResponse;
import com.wildlife.habitat.dto.PageResponse;
import com.wildlife.habitat.dto.ZoneResponse;
import com.wildlife.habitat.entity.AreaType;
import com.wildlife.habitat.entity.ProtectedArea;
import com.wildlife.habitat.exception.ConflictException;
import com.wildlife.habitat.exception.ResourceNotFoundException;
import com.wildlife.habitat.repository.AreaRepository;
import com.wildlife.habitat.repository.ZoneRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.wildlife.habitat.service.HabitatMapper.trimToNull;

@Service
@Transactional
public class AreaService {

    private final AreaRepository areaRepository;
    private final ZoneRepository zoneRepository;

    public AreaService(AreaRepository areaRepository, ZoneRepository zoneRepository) {
        this.areaRepository = areaRepository;
        this.zoneRepository = zoneRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AreaResponse> search(String q, AreaType type, String state, Pageable pageable) {
        Specification<ProtectedArea> spec = Specification.where(null);
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("code")), like)));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("areaType"), type));
        }
        if (state != null && !state.isBlank()) {
            String st = state.trim().toLowerCase(Locale.ROOT);
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("state")), st));
        }
        Map<Long, Long> zoneCounts = new HashMap<>();
        for (Object[] row : zoneRepository.countGroupedByArea()) {
            zoneCounts.put((Long) row[0], (Long) row[1]);
        }
        return PageResponse.of(areaRepository.findAll(spec, pageable),
                a -> HabitatMapper.toArea(a, zoneCounts.getOrDefault(a.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public AreaDetailResponse get(Long id) {
        ProtectedArea area = find(id);
        List<ZoneResponse> zones = zoneRepository.findByAreaIdOrderByNameAsc(id).stream()
                .map(HabitatMapper::toZone).toList();
        return HabitatMapper.toAreaDetail(area, zones);
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> zonesOf(Long areaId) {
        find(areaId);
        return zoneRepository.findByAreaIdOrderByNameAsc(areaId).stream().map(HabitatMapper::toZone).toList();
    }

    public AreaResponse create(AreaRequest req) {
        String code = req.code().trim().toUpperCase(Locale.ROOT);
        if (areaRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("An area with code " + code + " already exists");
        }
        ProtectedArea area = new ProtectedArea();
        apply(area, req, code);
        return HabitatMapper.toArea(areaRepository.save(area), 0);
    }

    public AreaResponse update(Long id, AreaRequest req) {
        ProtectedArea area = find(id);
        String code = req.code().trim().toUpperCase(Locale.ROOT);
        if (areaRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ConflictException("An area with code " + code + " already exists");
        }
        apply(area, req, code);
        return HabitatMapper.toArea(area, zoneRepository.countByAreaId(id));
    }

    public void delete(Long id) {
        ProtectedArea area = find(id);
        long zones = zoneRepository.countByAreaId(id);
        if (zones > 0) {
            throw new ConflictException("Cannot delete " + area.getName() + ": it still has " + zones + " zone(s)");
        }
        areaRepository.delete(area);
    }

    ProtectedArea find(Long id) {
        return areaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Area", id));
    }

    private static void apply(ProtectedArea a, AreaRequest req, String code) {
        a.setName(req.name().trim());
        a.setCode(code);
        a.setAreaType(req.areaType());
        a.setState(req.state().trim());
        a.setDistrict(trimToNull(req.district()));
        a.setTotalAreaSqKm(req.totalAreaSqKm());
        a.setEstablishedYear(req.establishedYear());
        a.setCenterLatitude(req.centerLatitude());
        a.setCenterLongitude(req.centerLongitude());
        a.setDescription(trimToNull(req.description()));
    }
}

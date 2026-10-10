package com.wildlife.patrol.service;

import com.wildlife.patrol.client.HabitatClient;
import com.wildlife.patrol.client.RemoteErrors;
import com.wildlife.patrol.client.ZoneDto;
import com.wildlife.patrol.dto.PageResponse;
import com.wildlife.patrol.dto.RouteRequest;
import com.wildlife.patrol.dto.RouteResponse;
import com.wildlife.patrol.dto.WaypointDto;
import com.wildlife.patrol.entity.PatrolRoute;
import com.wildlife.patrol.entity.Waypoint;
import com.wildlife.patrol.exception.BusinessRuleException;
import com.wildlife.patrol.exception.ConflictException;
import com.wildlife.patrol.exception.ResourceNotFoundException;
import com.wildlife.patrol.repository.RouteRepository;
import com.wildlife.patrol.repository.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.wildlife.patrol.service.PatrolMapper.trimToNull;

@Service
@Transactional
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    private final RouteRepository routeRepository;
    private final ScheduleRepository scheduleRepository;
    private final HabitatClient habitatClient;

    public RouteService(RouteRepository routeRepository, ScheduleRepository scheduleRepository,
                        HabitatClient habitatClient) {
        this.routeRepository = routeRepository;
        this.scheduleRepository = scheduleRepository;
        this.habitatClient = habitatClient;
    }

    @Transactional(readOnly = true)
    public PageResponse<RouteResponse> search(Long areaId, Long zoneId, Pageable pageable) {
        Specification<PatrolRoute> spec = Specification.where(null);
        if (areaId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("areaId"), areaId));
        if (zoneId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("zoneId"), zoneId));
        return PageResponse.of(routeRepository.findAll(spec, pageable), r -> PatrolMapper.toRoute(r));
    }

    @Transactional(readOnly = true)
    public RouteResponse get(Long id) {
        return PatrolMapper.toRoute(find(id));
    }

    public RouteResponse create(RouteRequest req) {
        PatrolRoute route = new PatrolRoute();
        List<String> warnings = apply(route, req);
        return PatrolMapper.toRoute(routeRepository.save(route), warnings);
    }

    public RouteResponse update(Long id, RouteRequest req) {
        PatrolRoute route = find(id);
        List<String> warnings = apply(route, req);
        return PatrolMapper.toRoute(route, warnings);
    }

    public void delete(Long id) {
        PatrolRoute route = find(id);
        if (scheduleRepository.countByRouteId(id) > 0) {
            throw new ConflictException("Route " + route.getName() + " is used by patrol schedules");
        }
        routeRepository.delete(route);
    }

    public PatrolRoute find(Long id) {
        return routeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Patrol route", id));
    }

    private List<String> apply(PatrolRoute route, RouteRequest req) {
        List<String> warnings = new ArrayList<>();
        String zoneName = null;
        try {
            ZoneDto zone = habitatClient.getZone(req.zoneId());
            if (zone.areaId() != null && !zone.areaId().equals(req.areaId())) {
                throw new BusinessRuleException("Zone " + req.zoneId() + " belongs to area " + zone.areaId()
                        + ", not area " + req.areaId());
            }
            zoneName = zone.name();
        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            if (RemoteErrors.isNotFound(e)) {
                throw new BusinessRuleException("Zone " + req.zoneId() + " does not exist in habitat-service");
            }
            log.warn("habitat-service unavailable while checking zone {}: {}", req.zoneId(), e.getMessage());
            warnings.add("Zone " + req.zoneId() + " could not be verified with habitat-service; zone name omitted.");
        }

        route.setName(req.name().trim());
        route.setAreaId(req.areaId());
        route.setZoneId(req.zoneId());
        route.setZoneName(zoneName);
        route.setDistanceKm(req.distanceKm());
        route.setEstimatedHours(req.estimatedHours());
        route.setDifficulty(req.difficulty());
        route.setDescription(trimToNull(req.description()));
        route.getWaypoints().clear();
        if (req.waypoints() != null) {
            req.waypoints().stream()
                    .sorted(Comparator.comparing(WaypointDto::sequence))
                    .forEach(w -> route.getWaypoints().add(
                            new Waypoint(w.sequence(), w.latitude(), w.longitude(), trimToNull(w.label()))));
        }
        return warnings;
    }
}

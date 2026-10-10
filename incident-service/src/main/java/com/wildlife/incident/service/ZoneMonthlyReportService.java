package com.wildlife.incident.service;

import com.wildlife.incident.client.HabitatClient;
import com.wildlife.incident.client.RemoteErrors;
import com.wildlife.incident.client.ZoneDto;
import com.wildlife.incident.dto.AssignedPersonnel;
import com.wildlife.incident.dto.MonthlyIncidentItem;
import com.wildlife.incident.dto.ZoneMonthlyIncidentReport;
import com.wildlife.incident.entity.Incident;
import com.wildlife.incident.entity.IncidentStatus;
import com.wildlife.incident.repository.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The mandatory example: a forest officer reviews all incidents in a protected zone for the current month.
 * GET /api/incidents/zone/{zoneId}/current-month?status=  (CONTRACTS.md 7.6)
 */
@Service
@Transactional(readOnly = true)
public class ZoneMonthlyReportService {

    private static final Logger log = LoggerFactory.getLogger(ZoneMonthlyReportService.class);

    private final IncidentRepository incidentRepository;
    private final IncidentService incidentService;
    private final HabitatClient habitatClient;
    private final Clock clock;

    public ZoneMonthlyReportService(IncidentRepository incidentRepository, IncidentService incidentService,
                                    HabitatClient habitatClient, Clock clock) {
        this.incidentRepository = incidentRepository;
        this.incidentService = incidentService;
        this.habitatClient = habitatClient;
        this.clock = clock;
    }

    public ZoneMonthlyIncidentReport currentMonth(Long zoneId, IncidentStatus statusFilter) {
        // 1. Current month in APP_TIMEZONE: [1st 00:00, 1st of next month 00:00)
        YearMonth month = YearMonth.from(LocalDate.now(clock));
        LocalDateTime periodStart = month.atDay(1).atStartOfDay();
        LocalDateTime periodEnd = month.plusMonths(1).atDay(1).atStartOfDay();

        // 2. Every incident in the zone this month (the summary always uses all of them)
        List<Incident> all = incidentRepository.findInZoneBetween(zoneId, periodStart, periodEnd);
        Map<String, Long> summary = new LinkedHashMap<>();
        for (IncidentStatus s : IncidentStatus.values()) summary.put(s.name(), 0L);
        for (Incident i : all) summary.merge(i.getStatus().name(), 1L, Long::sum);

        // 3. Apply the optional status filter to the list only
        List<Incident> shown = statusFilter == null ? all
                : all.stream().filter(i -> i.getStatus() == statusFilter).toList();

        // 4. Assigned personnel for the shown incidents, in one query
        Map<Long, List<AssignedPersonnel>> personnel =
                incidentService.personnelFor(shown.stream().map(Incident::getId).toList());
        List<MonthlyIncidentItem> items = shown.stream()
                .map(i -> IncidentMapper.toMonthlyItem(i, personnel.getOrDefault(i.getId(), List.of())))
                .toList();

        // 5. Zone and area names from habitat-service (degrades to a warning)
        List<String> warnings = new ArrayList<>();
        String zoneName = null;
        String areaName = null;
        Long areaId = all.isEmpty() ? null : all.get(0).getAreaId();
        try {
            ZoneDto zone = habitatClient.getZone(zoneId);
            zoneName = zone.name();
            areaName = zone.areaName();
            areaId = zone.areaId();
        } catch (Exception e) {
            if (RemoteErrors.isNotFound(e)) {
                warnings.add("Zone " + zoneId + " does not exist in habitat-service");
            } else {
                log.warn("habitat-service unavailable for zone {}: {}", zoneId, e.getMessage());
                warnings.add("Zone details could not be loaded from habitat-service; zone and area names omitted.");
            }
            if (zoneName == null && !all.isEmpty()) {
                zoneName = all.get(0).getZoneName(); // last known snapshot, if any
            }
        }

        return new ZoneMonthlyIncidentReport(zoneId, zoneName, areaId, areaName, month.toString(),
                periodStart, periodEnd, statusFilter, all.size(), summary, items, warnings);
    }
}

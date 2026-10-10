package com.wildlife.reporting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wildlife.reporting.client.HabitatClient;
import com.wildlife.reporting.client.IncidentClient;
import com.wildlife.reporting.client.PatrolClient;
import com.wildlife.reporting.client.ProgrammeClient;
import com.wildlife.reporting.client.RemoteErrors;
import com.wildlife.reporting.client.RescueClient;
import com.wildlife.reporting.client.ResourceClient;
import com.wildlife.reporting.client.WildlifeClient;
import com.wildlife.reporting.dto.DashboardResponse;
import com.wildlife.reporting.dto.ZoneReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Builds consolidated reports by calling each service with the caller's token (CONTRACTS.md 7.9).
 * A service that is down never breaks the report: its section is null and a warning is added.
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final HabitatClient habitat;
    private final WildlifeClient wildlife;
    private final ResourceClient resource;
    private final PatrolClient patrol;
    private final IncidentClient incident;
    private final RescueClient rescue;
    private final ProgrammeClient programme;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ReportService(HabitatClient habitat, WildlifeClient wildlife, ResourceClient resource, PatrolClient patrol,
                         IncidentClient incident, RescueClient rescue, ProgrammeClient programme,
                         ObjectMapper objectMapper, Clock clock) {
        this.habitat = habitat;
        this.wildlife = wildlife;
        this.resource = resource;
        this.patrol = patrol;
        this.incident = incident;
        this.rescue = rescue;
        this.programme = programme;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public DashboardResponse dashboard() {
        Map<String, Boolean> online = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        JsonNode habitatSummary = call("habitat", habitat::summary, online, warnings);
        JsonNode wildlifeSummary = call("wildlife", wildlife::summary, online, warnings);
        JsonNode resourceSummary = call("resource", resource::summary, online, warnings);
        JsonNode patrolSummary = call("patrol", patrol::summary, online, warnings);
        JsonNode incidentSummary = call("incident", () -> incident.summary(null, null), online, warnings);
        JsonNode rescueSummary = call("rescue", rescue::summary, online, warnings);
        JsonNode programmeSummary = call("programme", programme::summary, online, warnings);
        return new DashboardResponse(LocalDateTime.now(clock), habitatSummary, wildlifeSummary, resourceSummary,
                patrolSummary, incidentSummary, rescueSummary, programmeSummary, online, warnings);
    }

    /** IncidentSummary for one month plus a warnings array. */
    public JsonNode monthlyIncidents(Integer year, Integer month) {
        Map<String, Boolean> online = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        JsonNode summary = call("incident", () -> incident.summary(year, month), online, warnings);
        ObjectNode result = summary instanceof ObjectNode obj ? obj.deepCopy() : objectMapper.createObjectNode();
        if (summary == null) {
            if (year != null) result.put("year", year);
            if (month != null) result.put("month", month);
        }
        ArrayNode w = result.putArray("warnings");
        warnings.forEach(w::add);
        return result;
    }

    public ZoneReport zone(Long zoneId) {
        Map<String, Boolean> online = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        JsonNode zone = call("habitat", () -> habitat.getZone(zoneId), online, warnings);
        JsonNode animals = call("wildlife", () -> wildlife.animals(zoneId, 1), online, warnings);
        JsonNode incidents = call("incident", () -> incident.zoneCurrentMonth(zoneId), online, warnings);
        JsonNode water = call("habitat", () -> habitat.waterResources(zoneId, 100), online, warnings);
        Long animalCount = animals != null && animals.hasNonNull("totalElements")
                ? animals.get("totalElements").asLong() : null;
        JsonNode waterList = water != null ? water.get("content") : null;
        return new ZoneReport(zone, animalCount, incidents, waterList, warnings);
    }

    /** Calls one service; on failure records it as offline and adds a warning instead of throwing. */
    private JsonNode call(String service, Supplier<JsonNode> request, Map<String, Boolean> online,
                          List<String> warnings) {
        try {
            JsonNode result = request.get();
            online.putIfAbsent(service, true);
            return result;
        } catch (Exception e) {
            if (RemoteErrors.isClientError(e) && !RemoteErrors.isNotFound(e)) {
                // the service answered (e.g. 403): it is online, but this caller may not see the data
                online.putIfAbsent(service, true);
                warnings.add(service + "-service refused the request: " + RemoteErrors.messageOf(e));
                return null;
            }
            if (RemoteErrors.isNotFound(e)) {
                online.putIfAbsent(service, true);
                warnings.add(service + "-service: " + RemoteErrors.messageOf(e));
                return null;
            }
            log.warn("{}-service unavailable: {}", service, e.getMessage());
            online.put(service, false);
            warnings.add(service + "-service is unavailable; its section is empty.");
            return null;
        }
    }
}

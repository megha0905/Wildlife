package com.wildlife.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildlife.reporting.client.HabitatClient;
import com.wildlife.reporting.client.IncidentClient;
import com.wildlife.reporting.client.PatrolClient;
import com.wildlife.reporting.client.ProgrammeClient;
import com.wildlife.reporting.client.RescueClient;
import com.wildlife.reporting.client.ResourceClient;
import com.wildlife.reporting.client.WildlifeClient;
import com.wildlife.reporting.dto.DashboardResponse;
import com.wildlife.reporting.dto.ZoneReport;
import com.wildlife.reporting.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** A service being down must never break the dashboard (CONTRACTS.md 7.9). */
class ReportServiceTest {

    private final ObjectMapper json = new ObjectMapper();
    private HabitatClient habitat;
    private WildlifeClient wildlife;
    private IncidentClient incident;
    private ReportService service;

    @BeforeEach
    void setUp() throws Exception {
        habitat = mock(HabitatClient.class);
        wildlife = mock(WildlifeClient.class);
        incident = mock(IncidentClient.class);
        ResourceClient resource = mock(ResourceClient.class);
        PatrolClient patrol = mock(PatrolClient.class);
        RescueClient rescue = mock(RescueClient.class);
        ProgrammeClient programme = mock(ProgrammeClient.class);
        service = new ReportService(habitat, wildlife, resource, patrol, incident, rescue, programme, json,
                Clock.systemDefaultZone());

        when(habitat.summary()).thenReturn(json.readTree("{\"totalAreas\":3}"));
        when(wildlife.summary()).thenThrow(new RuntimeException("Connection refused"));
        when(resource.summary()).thenReturn(json.readTree("{\"totalStaff\":8}"));
        when(patrol.summary()).thenReturn(json.readTree("{\"schedulesToday\":1}"));
        when(incident.summary(any(), any())).thenReturn(json.readTree("{\"openIncidents\":5}"));
        when(rescue.summary()).thenReturn(json.readTree("{\"activeCases\":2}"));
        when(programme.summary()).thenReturn(json.readTree("{\"activeProgrammes\":2}"));
    }

    @Test
    void dashboardKeepsWorkingWhenOneServiceIsDown() {
        DashboardResponse d = service.dashboard();

        assertThat(d.habitat().get("totalAreas").asInt()).isEqualTo(3);
        assertThat(d.wildlife()).isNull();
        assertThat(d.servicesOnline()).containsEntry("wildlife", false).containsEntry("habitat", true);
        assertThat(d.servicesOnline()).hasSize(7);
        assertThat(d.warnings()).singleElement().asString().contains("wildlife-service is unavailable");
    }

    @Test
    void monthlyIncidentsAddsAWarningsArray() {
        JsonNode r = service.monthlyIncidents(2026, 10);
        assertThat(r.get("openIncidents").asInt()).isEqualTo(5);
        assertThat(r.get("warnings").isArray()).isTrue();
    }

    @Test
    void zoneReportReadsTotalsFromPages() throws Exception {
        when(habitat.getZone(1L)).thenReturn(json.readTree("{\"id\":1,\"name\":\"Mukki Core\"}"));
        when(wildlife.animals(any(), anyInt())).thenReturn(json.readTree("{\"content\":[],\"totalElements\":3}"));
        when(incident.zoneCurrentMonth(1L)).thenReturn(json.readTree("{\"totalIncidents\":5}"));
        when(habitat.waterResources(any(), anyInt())).thenReturn(json.readTree("{\"content\":[{\"id\":1}],\"totalElements\":1}"));

        ZoneReport z = service.zone(1L);

        assertThat(z.animalsInZone()).isEqualTo(3L);
        assertThat(z.waterResources().size()).isEqualTo(1);
        assertThat(z.incidentsThisMonth().get("totalIncidents").asInt()).isEqualTo(5);
        assertThat(z.warnings()).isEmpty();
    }
}

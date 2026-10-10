package com.wildlife.wildlife;

import com.wildlife.wildlife.client.HabitatClient;
import com.wildlife.wildlife.client.ZoneDto;
import com.wildlife.wildlife.exception.BusinessRuleException;
import com.wildlife.wildlife.service.ZoneLookup;
import feign.FeignException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** CONTRACTS.md 3.4: 404 -> 422, wrong area -> 422, unreachable -> accept with warning. */
class ZoneLookupTest {

    private final HabitatClient habitat = mock(HabitatClient.class);
    private final ZoneLookup lookup = new ZoneLookup(habitat);

    @Test
    void knownZoneFillsNameAndArea() {
        when(habitat.getZone(1L)).thenReturn(zone(1L, 1L, "Mukki Core"));

        ZoneLookup.ZoneCheck check = lookup.check(1L, null);

        assertThat(check.zoneName()).isEqualTo("Mukki Core");
        assertThat(check.areaId()).isEqualTo(1L);
        assertThat(check.warning()).isNull();
    }

    @Test
    void unknownZoneIs422() {
        FeignException notFound = mock(FeignException.class);
        when(notFound.status()).thenReturn(404);
        when(habitat.getZone(99L)).thenThrow(notFound);

        assertThatThrownBy(() -> lookup.check(99L, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void zoneInAnotherAreaIs422() {
        when(habitat.getZone(3L)).thenReturn(zone(3L, 2L, "Kohora Central"));

        assertThatThrownBy(() -> lookup.check(3L, 1L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void habitatDownGivesWarningInsteadOfFailure() {
        when(habitat.getZone(1L)).thenThrow(new RuntimeException("Connection refused"));

        ZoneLookup.ZoneCheck check = lookup.check(1L, 1L);

        assertThat(check.warning()).contains("could not be verified");
        assertThat(check.zoneName()).isNull();
        assertThat(check.areaId()).isEqualTo(1L);
    }

    private static ZoneDto zone(Long id, Long areaId, String name) {
        return new ZoneDto(id, areaId, "Area " + areaId, name, "Z" + id, "CORE", 10.0, 22.0, 80.0, null);
    }
}

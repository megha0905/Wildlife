package com.wildlife.wildlife.service;

import com.wildlife.wildlife.client.HabitatClient;
import com.wildlife.wildlife.client.RemoteErrors;
import com.wildlife.wildlife.client.ZoneDto;
import com.wildlife.wildlife.exception.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates a zone against habitat-service (CONTRACTS.md 7.3 rules):
 * 404 -> 422, zone in a different area -> 422, habitat unreachable -> accept with a warning.
 */
@Component
public class ZoneLookup {

    private static final Logger log = LoggerFactory.getLogger(ZoneLookup.class);

    private final HabitatClient habitatClient;

    public ZoneLookup(HabitatClient habitatClient) {
        this.habitatClient = habitatClient;
    }

    /** Result of a lookup: resolved area id, zone name snapshot and an optional warning. */
    public record ZoneCheck(Long areaId, String zoneName, String warning) {
    }

    public ZoneCheck check(Long zoneId, Long areaId) {
        if (zoneId == null) {
            return new ZoneCheck(areaId, null, null);
        }
        ZoneDto zone;
        try {
            zone = habitatClient.getZone(zoneId);
        } catch (Exception e) {
            if (RemoteErrors.isNotFound(e)) {
                throw new BusinessRuleException("Zone " + zoneId + " does not exist in habitat-service");
            }
            log.warn("habitat-service unavailable while checking zone {}: {}", zoneId, e.getMessage());
            return new ZoneCheck(areaId, null,
                    "Zone " + zoneId + " could not be verified with habitat-service; zone name omitted.");
        }
        if (areaId != null && zone.areaId() != null && !areaId.equals(zone.areaId())) {
            throw new BusinessRuleException("Zone " + zoneId + " belongs to area " + zone.areaId()
                    + ", not area " + areaId);
        }
        return new ZoneCheck(zone.areaId() != null ? zone.areaId() : areaId, zone.name(), null);
    }
}

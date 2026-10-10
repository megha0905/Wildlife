package com.wildlife.habitat.service;

import com.wildlife.habitat.dto.AreaDetailResponse;
import com.wildlife.habitat.dto.AreaResponse;
import com.wildlife.habitat.dto.HabitatResponse;
import com.wildlife.habitat.dto.WaterResourceResponse;
import com.wildlife.habitat.dto.ZoneResponse;
import com.wildlife.habitat.entity.Habitat;
import com.wildlife.habitat.entity.ProtectedArea;
import com.wildlife.habitat.entity.WaterResource;
import com.wildlife.habitat.entity.Zone;

import java.util.List;

/** Entity to DTO conversion. Call inside a transaction (lazy relations). */
final class HabitatMapper {

    private HabitatMapper() {
    }

    static AreaResponse toArea(ProtectedArea a, long zoneCount) {
        return new AreaResponse(a.getId(), a.getName(), a.getCode(), a.getAreaType(), a.getState(), a.getDistrict(),
                a.getTotalAreaSqKm(), a.getEstablishedYear(), a.getCenterLatitude(), a.getCenterLongitude(),
                a.getDescription(), zoneCount);
    }

    static AreaDetailResponse toAreaDetail(ProtectedArea a, List<ZoneResponse> zones) {
        return new AreaDetailResponse(a.getId(), a.getName(), a.getCode(), a.getAreaType(), a.getState(),
                a.getDistrict(), a.getTotalAreaSqKm(), a.getEstablishedYear(), a.getCenterLatitude(),
                a.getCenterLongitude(), a.getDescription(), zones.size(), zones);
    }

    static ZoneResponse toZone(Zone z) {
        return new ZoneResponse(z.getId(), z.getArea().getId(), z.getArea().getName(), z.getName(), z.getCode(),
                z.getZoneType(), z.getAreaSqKm(), z.getCenterLatitude(), z.getCenterLongitude(), z.getDescription());
    }

    static HabitatResponse toHabitat(Habitat h) {
        Zone z = h.getZone();
        return new HabitatResponse(h.getId(), z.getId(), z.getName(), z.getArea().getId(), h.getName(),
                h.getHabitatType(), h.getCondition(), h.getAreaSqKm(), h.getLatitude(), h.getLongitude(),
                h.getDominantVegetation(), h.getLastSurveyDate(), h.getNotes());
    }

    static WaterResourceResponse toWater(WaterResource w) {
        Zone z = w.getZone();
        return new WaterResourceResponse(w.getId(), z.getId(), z.getName(), z.getArea().getId(), w.getName(),
                w.getSourceType(), w.getStatus(), w.isPerennial(), w.getLatitude(), w.getLongitude(),
                w.getCapacityCubicM(), w.getLastInspectedAt(), w.getNotes());
    }

    static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

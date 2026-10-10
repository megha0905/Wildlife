package com.wildlife.patrol.service;

import com.wildlife.patrol.dto.LogResponse;
import com.wildlife.patrol.dto.RouteResponse;
import com.wildlife.patrol.dto.ScheduleResponse;
import com.wildlife.patrol.dto.StaffRef;
import com.wildlife.patrol.dto.TeamResponse;
import com.wildlife.patrol.dto.WaypointDto;
import com.wildlife.patrol.entity.PatrolLog;
import com.wildlife.patrol.entity.PatrolRoute;
import com.wildlife.patrol.entity.PatrolSchedule;
import com.wildlife.patrol.entity.PatrolTeam;

import java.util.List;

final class PatrolMapper {

    private PatrolMapper() {
    }

    static TeamResponse toTeam(PatrolTeam t, List<String> warnings) {
        List<StaffRef> members = t.getMembers().stream()
                .map(m -> new StaffRef(m.getStaffId(), null, m.getStaffName(), m.getDesignation(), null))
                .toList();
        return new TeamResponse(t.getId(), t.getName(), t.getAreaId(), t.getLeaderStaffId(), t.getLeaderName(),
                members, t.isActive(), warnings == null ? List.of() : warnings);
    }

    static TeamResponse toTeam(PatrolTeam t) {
        return toTeam(t, List.of());
    }

    static RouteResponse toRoute(PatrolRoute r, List<String> warnings) {
        List<WaypointDto> points = r.getWaypoints().stream()
                .map(w -> new WaypointDto(w.getSequence(), w.getLatitude(), w.getLongitude(), w.getLabel()))
                .toList();
        return new RouteResponse(r.getId(), r.getName(), r.getAreaId(), r.getZoneId(), r.getZoneName(),
                r.getDistanceKm(), r.getEstimatedHours(), r.getDifficulty(), points, r.getDescription(),
                warnings == null ? List.of() : warnings);
    }

    static RouteResponse toRoute(PatrolRoute r) {
        return toRoute(r, List.of());
    }

    static ScheduleResponse toSchedule(PatrolSchedule s, List<String> warnings) {
        PatrolRoute r = s.getRoute();
        return new ScheduleResponse(s.getId(), code(s), s.getTeam().getId(), s.getTeam().getName(), r.getId(),
                r.getName(), r.getAreaId(), r.getZoneId(), r.getZoneName(), s.getPatrolDate(), s.getShift(),
                s.getStartTime(), s.getEndTime(), s.getStatus(), s.getVehicleId(), List.copyOf(s.getAllocationIds()),
                s.getObjective(), s.getStartedAt(), s.getCompletedAt(), warnings == null ? List.of() : warnings);
    }

    static ScheduleResponse toSchedule(PatrolSchedule s) {
        return toSchedule(s, List.of());
    }

    static LogResponse toLog(PatrolLog l, List<String> warnings) {
        return new LogResponse(l.getId(), l.getSchedule().getId(), l.getLoggedAt(), l.getObservationType(),
                l.getLatitude(), l.getLongitude(), l.getDescription(), l.getAnimalTagId(), l.getSeverityHint(),
                l.getReportedByUserId(), l.getReportedByName(), l.getIncidentId(), l.getCreatedAt(),
                warnings == null ? List.of() : warnings);
    }

    static LogResponse toLog(PatrolLog l) {
        return toLog(l, List.of());
    }

    static String code(PatrolSchedule s) {
        return "PS-" + s.getId();
    }

    static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

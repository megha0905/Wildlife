package com.wildlife.incident.service;

import com.wildlife.incident.entity.IncidentStatus;
import com.wildlife.incident.exception.BusinessRuleException;
import com.wildlife.incident.exception.ConflictException;
import com.wildlife.incident.exception.ForbiddenOperationException;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Incident workflow (CONTRACTS.md 6.1):
 * REPORTED -> VERIFIED -> IN_PROGRESS -> RESOLVED -> CLOSED, one step at a time.
 * Pure Java so it is easy to read and unit-test.
 */
public final class IncidentWorkflow {

    /** The only legal next status for each status (null = final). */
    private static final Map<IncidentStatus, IncidentStatus> NEXT = Map.of(
            IncidentStatus.REPORTED, IncidentStatus.VERIFIED,
            IncidentStatus.VERIFIED, IncidentStatus.IN_PROGRESS,
            IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED,
            IncidentStatus.RESOLVED, IncidentStatus.CLOSED);

    /** Roles allowed to make the move INTO each status. */
    private static final Map<IncidentStatus, Set<String>> ROLES_FOR_TARGET = Map.of(
            IncidentStatus.VERIFIED, Set.of("ADMIN", "FOREST_OFFICER"),
            IncidentStatus.IN_PROGRESS, Set.of("ADMIN", "FOREST_OFFICER"),
            IncidentStatus.RESOLVED, Set.of("ADMIN", "FOREST_OFFICER", "RANGER", "VET"),
            IncidentStatus.CLOSED, Set.of("ADMIN", "FOREST_OFFICER"));

    private IncidentWorkflow() {
    }

    public static boolean isLegal(IncidentStatus from, IncidentStatus to) {
        return to != null && to == NEXT.get(from);
    }

    /**
     * Checks a requested move. Order matters: illegal move (409) first, then role (403), then
     * the move's own requirements (422).
     *
     * @param activeAssignments number of active assignments on the incident
     * @param note              the note sent with the request (resolution notes for RESOLVED)
     */
    public static void check(String incidentCode, IncidentStatus from, IncidentStatus to, List<String> userRoles,
                             long activeAssignments, String note) {
        if (!isLegal(from, to)) {
            throw new ConflictException("Cannot move incident " + incidentCode + " from " + from + " to " + to);
        }
        Set<String> allowed = ROLES_FOR_TARGET.get(to);
        if (userRoles.stream().noneMatch(allowed::contains)) {
            throw new ForbiddenOperationException("Only " + String.join(", ", allowed.stream().sorted().toList())
                    + " may move an incident to " + to);
        }
        if (to == IncidentStatus.IN_PROGRESS && activeAssignments == 0) {
            throw new BusinessRuleException("Assign at least one staff member before starting work on " + incidentCode);
        }
        if (to == IncidentStatus.RESOLVED && (note == null || note.isBlank())) {
            throw new BusinessRuleException("Resolution notes are required to resolve " + incidentCode);
        }
    }

    public static boolean isOpen(IncidentStatus status) {
        return status == IncidentStatus.REPORTED || status == IncidentStatus.VERIFIED
                || status == IncidentStatus.IN_PROGRESS;
    }
}

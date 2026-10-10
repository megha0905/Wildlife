package com.wildlife.patrol.service;

import com.wildlife.patrol.entity.PatrolStatus;
import com.wildlife.patrol.entity.Shift;
import com.wildlife.patrol.exception.BusinessRuleException;
import com.wildlife.patrol.exception.ConflictException;

import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Pure rules for patrol schedules (CONTRACTS.md 6.3). No Spring, easy to unit-test. */
public final class PatrolWorkflow {

    private static final Map<PatrolStatus, Set<PatrolStatus>> MOVES = Map.of(
            PatrolStatus.SCHEDULED, EnumSet.of(PatrolStatus.IN_PROGRESS, PatrolStatus.CANCELLED, PatrolStatus.MISSED),
            PatrolStatus.IN_PROGRESS, EnumSet.of(PatrolStatus.COMPLETED),
            PatrolStatus.COMPLETED, EnumSet.noneOf(PatrolStatus.class),
            PatrolStatus.CANCELLED, EnumSet.noneOf(PatrolStatus.class),
            PatrolStatus.MISSED, EnumSet.noneOf(PatrolStatus.class));

    private PatrolWorkflow() {
    }

    public static boolean canMove(PatrolStatus from, PatrolStatus to) {
        return MOVES.get(from).contains(to);
    }

    public static void requireMove(String code, PatrolStatus from, PatrolStatus to) {
        if (!canMove(from, to)) {
            throw new ConflictException("Cannot move patrol " + code + " from " + from + " to " + to);
        }
    }

    /** Logs can only be added to a patrol that has started. */
    public static boolean acceptsLogs(PatrolStatus status) {
        return status == PatrolStatus.IN_PROGRESS || status == PatrolStatus.COMPLETED;
    }

    /** Start and end must differ; only a NIGHT patrol may end after midnight (end earlier than start). */
    public static void requireValidTimes(Shift shift, LocalTime start, LocalTime end) {
        if (start.equals(end)) {
            throw new BusinessRuleException("startTime and endTime must be different");
        }
        if (end.isBefore(start) && shift != Shift.NIGHT) {
            throw new BusinessRuleException("Only NIGHT patrols may end after midnight; check startTime and endTime");
        }
    }
}

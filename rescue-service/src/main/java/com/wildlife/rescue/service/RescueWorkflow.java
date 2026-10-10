package com.wildlife.rescue.service;

import com.wildlife.rescue.entity.RescueStatus;
import com.wildlife.rescue.exception.BusinessRuleException;
import com.wildlife.rescue.exception.ConflictException;
import com.wildlife.rescue.exception.ForbiddenOperationException;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rescue workflow (CONTRACTS.md 6.2):
 * REGISTERED -> UNDER_TREATMENT -> REHABILITATING -> TRANSFERRED | RELEASED. Pure Java, easy to unit-test.
 */
public final class RescueWorkflow {

    private static final Map<RescueStatus, Set<RescueStatus>> MOVES = Map.of(
            RescueStatus.REGISTERED, EnumSet.of(RescueStatus.UNDER_TREATMENT),
            RescueStatus.UNDER_TREATMENT, EnumSet.of(RescueStatus.REHABILITATING),
            RescueStatus.REHABILITATING, EnumSet.of(RescueStatus.TRANSFERRED, RescueStatus.RELEASED),
            RescueStatus.TRANSFERRED, EnumSet.noneOf(RescueStatus.class),
            RescueStatus.RELEASED, EnumSet.noneOf(RescueStatus.class));

    private static final Map<RescueStatus, Set<String>> ROLES_FOR_TARGET = Map.of(
            RescueStatus.UNDER_TREATMENT, Set.of("ADMIN", "FOREST_OFFICER", "VET"),
            RescueStatus.REHABILITATING, Set.of("ADMIN", "VET"),
            RescueStatus.TRANSFERRED, Set.of("ADMIN", "FOREST_OFFICER", "VET"),
            RescueStatus.RELEASED, Set.of("ADMIN", "FOREST_OFFICER"));

    private RescueWorkflow() {
    }

    public static boolean isLegal(RescueStatus from, RescueStatus to) {
        return to != null && MOVES.get(from).contains(to);
    }

    public static boolean isFinal(RescueStatus status) {
        return status == RescueStatus.TRANSFERRED || status == RescueStatus.RELEASED;
    }

    /**
     * Checks a requested move: illegal (409), then role (403), then requirements (422).
     *
     * @param treatmentCount       treatments recorded so far
     * @param latestFitForRelease  fitForRelease of the latest rehab assessment (null if none)
     * @param hasTransfer          the request carries a transfer object
     * @param hasRelease           the request carries a release object
     */
    public static void check(String caseNumber, RescueStatus from, RescueStatus to, List<String> userRoles,
                             long treatmentCount, Boolean latestFitForRelease, boolean hasTransfer, boolean hasRelease) {
        if (!isLegal(from, to)) {
            throw new ConflictException("Cannot move rescue case " + caseNumber + " from " + from + " to " + to);
        }
        Set<String> allowed = ROLES_FOR_TARGET.get(to);
        if (userRoles.stream().noneMatch(allowed::contains)) {
            throw new ForbiddenOperationException("Only " + String.join(", ", allowed.stream().sorted().toList())
                    + " may move a rescue case to " + to);
        }
        switch (to) {
            case REHABILITATING -> {
                if (treatmentCount == 0) {
                    throw new BusinessRuleException("Record at least one treatment before rehabilitation");
                }
            }
            case TRANSFERRED -> {
                if (!hasTransfer) {
                    throw new BusinessRuleException("Transfer details (destinationFacility, reason, transferDate) are required");
                }
            }
            case RELEASED -> {
                if (!hasRelease) {
                    throw new BusinessRuleException("Release details (releaseDate, area, zone, location) are required");
                }
                if (!Boolean.TRUE.equals(latestFitForRelease)) {
                    throw new BusinessRuleException("The latest rehabilitation assessment must mark the animal fit for release");
                }
            }
            default -> { }
        }
    }
}

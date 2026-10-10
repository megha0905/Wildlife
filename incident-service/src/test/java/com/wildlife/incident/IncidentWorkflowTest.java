package com.wildlife.incident;

import com.wildlife.incident.entity.IncidentStatus;
import com.wildlife.incident.exception.BusinessRuleException;
import com.wildlife.incident.exception.ConflictException;
import com.wildlife.incident.exception.ForbiddenOperationException;
import com.wildlife.incident.service.IncidentWorkflow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static com.wildlife.incident.entity.IncidentStatus.CLOSED;
import static com.wildlife.incident.entity.IncidentStatus.IN_PROGRESS;
import static com.wildlife.incident.entity.IncidentStatus.REPORTED;
import static com.wildlife.incident.entity.IncidentStatus.RESOLVED;
import static com.wildlife.incident.entity.IncidentStatus.VERIFIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** CONTRACTS.md 6.1: REPORTED -> VERIFIED -> IN_PROGRESS -> RESOLVED -> CLOSED. */
class IncidentWorkflowTest {

    private static final List<String> OFFICER = List.of("FOREST_OFFICER");
    private static final List<String> RANGER = List.of("RANGER");
    private static final List<String> ANALYST = List.of("ANALYST");

    @Test
    void happyPathByAnOfficer() {
        assertThatCode(() -> {
            IncidentWorkflow.check("INC-1", REPORTED, VERIFIED, OFFICER, 0, null);
            IncidentWorkflow.check("INC-1", VERIFIED, IN_PROGRESS, OFFICER, 1, null);
            IncidentWorkflow.check("INC-1", IN_PROGRESS, RESOLVED, OFFICER, 1, "Snare removed");
            IncidentWorkflow.check("INC-1", RESOLVED, CLOSED, OFFICER, 1, null);
        }).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} -> {1} is illegal")
    @CsvSource({
            "REPORTED, IN_PROGRESS", "REPORTED, RESOLVED", "REPORTED, CLOSED", "REPORTED, REPORTED",
            "VERIFIED, REPORTED", "VERIFIED, RESOLVED", "IN_PROGRESS, VERIFIED", "IN_PROGRESS, CLOSED",
            "RESOLVED, IN_PROGRESS", "CLOSED, REPORTED", "CLOSED, CLOSED"
    })
    void skippingOrGoingBackIs409(IncidentStatus from, IncidentStatus to) {
        assertThat(IncidentWorkflow.isLegal(from, to)).isFalse();
        assertThatThrownBy(() -> IncidentWorkflow.check("INC-2026-0004", from, to, List.of("ADMIN"), 1, "note"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Cannot move incident INC-2026-0004 from " + from + " to " + to);
    }

    @Test
    void rangerCanResolveButNotVerifyOrClose() {
        assertThatCode(() -> IncidentWorkflow.check("INC-1", IN_PROGRESS, RESOLVED, RANGER, 1, "done"))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> IncidentWorkflow.check("INC-1", REPORTED, VERIFIED, RANGER, 0, null))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> IncidentWorkflow.check("INC-1", RESOLVED, CLOSED, RANGER, 1, null))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void vetCanResolve() {
        assertThatCode(() -> IncidentWorkflow.check("INC-1", IN_PROGRESS, RESOLVED, List.of("VET"), 1, "Treated"))
                .doesNotThrowAnyException();
    }

    @Test
    void analystCanMakeNoMoves() {
        assertThatThrownBy(() -> IncidentWorkflow.check("INC-1", IN_PROGRESS, RESOLVED, ANALYST, 1, "x"))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void startingWorkNeedsAnActiveAssignment() {
        assertThatThrownBy(() -> IncidentWorkflow.check("INC-1", VERIFIED, IN_PROGRESS, OFFICER, 0, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Assign at least one");
    }

    @Test
    void resolvingNeedsNotes() {
        assertThatThrownBy(() -> IncidentWorkflow.check("INC-1", IN_PROGRESS, RESOLVED, OFFICER, 1, "  "))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Resolution notes");
    }

    @Test
    void illegalMoveIsReportedBeforeRole() {
        // a ranger asking for an illegal move gets 409 (the move itself is wrong), not 403
        assertThatThrownBy(() -> IncidentWorkflow.check("INC-1", REPORTED, CLOSED, RANGER, 0, null))
                .isInstanceOf(ConflictException.class);
    }
}

package com.wildlife.rescue;

import com.wildlife.rescue.entity.RescueStatus;
import com.wildlife.rescue.exception.BusinessRuleException;
import com.wildlife.rescue.exception.ConflictException;
import com.wildlife.rescue.exception.ForbiddenOperationException;
import com.wildlife.rescue.service.RescueWorkflow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static com.wildlife.rescue.entity.RescueStatus.REGISTERED;
import static com.wildlife.rescue.entity.RescueStatus.REHABILITATING;
import static com.wildlife.rescue.entity.RescueStatus.RELEASED;
import static com.wildlife.rescue.entity.RescueStatus.TRANSFERRED;
import static com.wildlife.rescue.entity.RescueStatus.UNDER_TREATMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** CONTRACTS.md 6.2: REGISTERED -> UNDER_TREATMENT -> REHABILITATING -> TRANSFERRED | RELEASED. */
class RescueWorkflowTest {

    private static final List<String> VET = List.of("VET");
    private static final List<String> OFFICER = List.of("FOREST_OFFICER");

    @Test
    void vetTreatsAndRehabilitates() {
        assertThatCode(() -> {
            RescueWorkflow.check("RSC-1", REGISTERED, UNDER_TREATMENT, VET, 0, null, false, false);
            RescueWorkflow.check("RSC-1", UNDER_TREATMENT, REHABILITATING, VET, 1, null, false, false);
            RescueWorkflow.check("RSC-1", REHABILITATING, TRANSFERRED, VET, 1, null, true, false);
        }).doesNotThrowAnyException();
    }

    @Test
    void officerReleasesWhenFit() {
        assertThatCode(() -> RescueWorkflow.check("RSC-2", REHABILITATING, RELEASED, OFFICER, 1, true, false, true))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} -> {1} is illegal")
    @CsvSource({
            "REGISTERED, REHABILITATING", "REGISTERED, RELEASED", "REGISTERED, TRANSFERRED",
            "UNDER_TREATMENT, RELEASED", "UNDER_TREATMENT, TRANSFERRED", "UNDER_TREATMENT, REGISTERED",
            "REHABILITATING, UNDER_TREATMENT", "RELEASED, REHABILITATING", "TRANSFERRED, RELEASED",
            "RELEASED, RELEASED"
    })
    void illegalMovesAre409(RescueStatus from, RescueStatus to) {
        assertThat(RescueWorkflow.isLegal(from, to)).isFalse();
        assertThatThrownBy(() -> RescueWorkflow.check("RSC-2026-0002", from, to, List.of("ADMIN"), 5, true, true, true))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Cannot move rescue case RSC-2026-0002 from " + from + " to " + to);
    }

    @Test
    void transferredAndReleasedAreFinal() {
        assertThat(RescueWorkflow.isFinal(TRANSFERRED)).isTrue();
        assertThat(RescueWorkflow.isFinal(RELEASED)).isTrue();
        assertThat(RescueWorkflow.isFinal(REHABILITATING)).isFalse();
    }

    @Test
    void rehabilitationNeedsATreatment() {
        assertThatThrownBy(() -> RescueWorkflow.check("RSC-1", UNDER_TREATMENT, REHABILITATING, VET, 0, null, false, false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("treatment");
    }

    @Test
    void transferNeedsDetails() {
        assertThatThrownBy(() -> RescueWorkflow.check("RSC-1", REHABILITATING, TRANSFERRED, VET, 1, null, false, false))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void releaseNeedsDetailsAndAFitAssessment() {
        assertThatThrownBy(() -> RescueWorkflow.check("RSC-1", REHABILITATING, RELEASED, OFFICER, 1, true, false, false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Release details");
        assertThatThrownBy(() -> RescueWorkflow.check("RSC-1", REHABILITATING, RELEASED, OFFICER, 1, false, false, true))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("fit for release");
        assertThatThrownBy(() -> RescueWorkflow.check("RSC-1", REHABILITATING, RELEASED, OFFICER, 1, null, false, true))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rolesPerMove() {
        // a vet cannot release; an officer cannot move to rehabilitation; a ranger can do neither
        assertThatThrownBy(() -> RescueWorkflow.check("RSC-1", REHABILITATING, RELEASED, VET, 1, true, false, true))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> RescueWorkflow.check("RSC-1", UNDER_TREATMENT, REHABILITATING, OFFICER, 1, null, false, false))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> RescueWorkflow.check("RSC-1", REGISTERED, UNDER_TREATMENT, List.of("RANGER"), 0, null, false, false))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}

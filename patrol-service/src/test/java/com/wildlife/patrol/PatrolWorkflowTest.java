package com.wildlife.patrol;

import com.wildlife.patrol.entity.PatrolStatus;
import com.wildlife.patrol.entity.Shift;
import com.wildlife.patrol.exception.BusinessRuleException;
import com.wildlife.patrol.exception.ConflictException;
import com.wildlife.patrol.service.PatrolWorkflow;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatrolWorkflowTest {

    @Test
    void legalMoves() {
        assertThat(PatrolWorkflow.canMove(PatrolStatus.SCHEDULED, PatrolStatus.IN_PROGRESS)).isTrue();
        assertThat(PatrolWorkflow.canMove(PatrolStatus.SCHEDULED, PatrolStatus.CANCELLED)).isTrue();
        assertThat(PatrolWorkflow.canMove(PatrolStatus.SCHEDULED, PatrolStatus.MISSED)).isTrue();
        assertThat(PatrolWorkflow.canMove(PatrolStatus.IN_PROGRESS, PatrolStatus.COMPLETED)).isTrue();
    }

    @Test
    void illegalMovesAre409() {
        assertThat(PatrolWorkflow.canMove(PatrolStatus.SCHEDULED, PatrolStatus.COMPLETED)).isFalse();
        assertThat(PatrolWorkflow.canMove(PatrolStatus.IN_PROGRESS, PatrolStatus.CANCELLED)).isFalse();
        assertThat(PatrolWorkflow.canMove(PatrolStatus.COMPLETED, PatrolStatus.IN_PROGRESS)).isFalse();
        assertThatThrownBy(() -> PatrolWorkflow.requireMove("PS-1", PatrolStatus.CANCELLED, PatrolStatus.SCHEDULED))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("PS-1");
    }

    @Test
    void logsOnlyOnceStarted() {
        assertThat(PatrolWorkflow.acceptsLogs(PatrolStatus.SCHEDULED)).isFalse();
        assertThat(PatrolWorkflow.acceptsLogs(PatrolStatus.IN_PROGRESS)).isTrue();
        assertThat(PatrolWorkflow.acceptsLogs(PatrolStatus.COMPLETED)).isTrue();
        assertThat(PatrolWorkflow.acceptsLogs(PatrolStatus.CANCELLED)).isFalse();
    }

    @Test
    void onlyNightPatrolsMayCrossMidnight() {
        assertThatCode(() -> PatrolWorkflow.requireValidTimes(Shift.NIGHT, LocalTime.of(22, 0), LocalTime.of(4, 0)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> PatrolWorkflow.requireValidTimes(Shift.MORNING, LocalTime.of(10, 0), LocalTime.of(6, 0)))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> PatrolWorkflow.requireValidTimes(Shift.EVENING, LocalTime.of(15, 0), LocalTime.of(15, 0)))
                .isInstanceOf(BusinessRuleException.class);
    }
}

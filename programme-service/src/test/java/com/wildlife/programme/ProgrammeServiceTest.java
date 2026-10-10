package com.wildlife.programme;

import com.wildlife.programme.dto.ProgressUpdateRequest;
import com.wildlife.programme.dto.ProgressUpdateResponse;
import com.wildlife.programme.entity.Programme;
import com.wildlife.programme.entity.ProgrammeStatus;
import com.wildlife.programme.entity.ProgressUpdate;
import com.wildlife.programme.entity.Target;
import com.wildlife.programme.exception.BusinessRuleException;
import com.wildlife.programme.exception.ConflictException;
import com.wildlife.programme.repository.ActivityRepository;
import com.wildlife.programme.repository.ProgrammeRepository;
import com.wildlife.programme.repository.ProgressUpdateRepository;
import com.wildlife.programme.repository.TargetRepository;
import com.wildlife.programme.service.ProgrammeService;
import com.wildlife.programme.service.StaffNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProgrammeServiceTest {

    private ProgrammeRepository programmes;
    private TargetRepository targets;
    private ProgressUpdateRepository progress;
    private ProgrammeService service;
    private Programme drive;

    @BeforeEach
    void setUp() {
        programmes = mock(ProgrammeRepository.class);
        targets = mock(TargetRepository.class);
        progress = mock(ProgressUpdateRepository.class);
        StaffNames staff = mock(StaffNames.class);
        when(staff.lookup(any())).thenReturn(new StaffNames.Lookup(Map.of(), null));
        service = new ProgrammeService(programmes, mock(ActivityRepository.class), targets, progress, staff,
                Clock.systemDefaultZone());

        drive = new Programme();
        drive.setId(1L);
        drive.setCode("KNH-ASD-2026");
        drive.setStatus(ProgrammeStatus.ACTIVE);
        when(programmes.findById(1L)).thenReturn(Optional.of(drive));
        when(progress.save(any(ProgressUpdate.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void progressUpdateMovesTheTargetAndRemembersThePreviousValue() {
        Target snares = new Target();
        snares.setId(1L);
        snares.setProgramme(drive);
        snares.setMetricName("Snares found per month");
        snares.setCurrentValue(21.0);
        when(targets.findByIdAndProgrammeId(1L, 1L)).thenReturn(Optional.of(snares));

        ProgressUpdateResponse r = service.addProgress(1L,
                new ProgressUpdateRequest(1L, LocalDate.now(), 12.0, "Down to 12"));

        assertThat(snares.getCurrentValue()).isEqualTo(12.0);
        assertThat(r.previousValue()).isEqualTo(21.0);
        assertThat(r.newValue()).isEqualTo(12.0);
        assertThat(r.targetMetricName()).isEqualTo("Snares found per month");
    }

    @Test
    void targetUpdateNeedsANewValue() {
        assertThatThrownBy(() -> service.addProgress(1L, new ProgressUpdateRequest(1L, LocalDate.now(), null, "x")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void completedProgrammeIsReadOnly() {
        drive.setStatus(ProgrammeStatus.COMPLETED);
        assertThatThrownBy(() -> service.addProgress(1L, new ProgressUpdateRequest(null, LocalDate.now(), null, "x")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void statusMoves() {
        drive.setStatus(ProgrammeStatus.PLANNED);
        assertThatThrownBy(() -> service.changeStatus(1L, ProgrammeStatus.COMPLETED))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("PLANNED to COMPLETED");
        assertThat(service.changeStatus(1L, ProgrammeStatus.ACTIVE).status()).isEqualTo(ProgrammeStatus.ACTIVE);
        assertThat(service.changeStatus(1L, ProgrammeStatus.ON_HOLD).status()).isEqualTo(ProgrammeStatus.ON_HOLD);
        assertThat(service.changeStatus(1L, ProgrammeStatus.ACTIVE).status()).isEqualTo(ProgrammeStatus.ACTIVE);
    }
}

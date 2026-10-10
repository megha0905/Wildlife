package com.wildlife.rescue;

import com.wildlife.rescue.client.AnimalStatusUpdate;
import com.wildlife.rescue.client.IncidentClient;
import com.wildlife.rescue.client.WildlifeClient;
import com.wildlife.rescue.dto.ReleaseInfo;
import com.wildlife.rescue.dto.RescueResponse;
import com.wildlife.rescue.dto.RescueTransitionRequest;
import com.wildlife.rescue.entity.HealthStatus;
import com.wildlife.rescue.entity.RehabProgress;
import com.wildlife.rescue.entity.RescueCase;
import com.wildlife.rescue.entity.RescueCause;
import com.wildlife.rescue.entity.RescueStatus;
import com.wildlife.rescue.repository.RehabProgressRepository;
import com.wildlife.rescue.repository.RescueHistoryRepository;
import com.wildlife.rescue.repository.RescueRepository;
import com.wildlife.rescue.repository.TreatmentRepository;
import com.wildlife.rescue.service.RescueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.wildlife.rescue.config.AuthUser;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Release syncs the animal back into the wild (CONTRACTS.md 9.4); wildlife failures only warn. */
class RescueServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-10T06:00:00Z"), ZoneId.of("Asia/Kolkata"));
    private RescueRepository rescues;
    private RehabProgressRepository rehab;
    private WildlifeClient wildlife;
    private RescueService service;
    private RescueCase cub;

    @BeforeEach
    void setUp() {
        rescues = mock(RescueRepository.class);
        rehab = mock(RehabProgressRepository.class);
        wildlife = mock(WildlifeClient.class);
        TreatmentRepository treatments = mock(TreatmentRepository.class);
        service = new RescueService(rescues, treatments, rehab, mock(RescueHistoryRepository.class), wildlife,
                mock(IncidentClient.class), clock);

        cub = new RescueCase();
        cub.setId(2L);
        cub.setCaseNumber("RSC-2026-0002");
        cub.setAnimalId(6L);
        cub.setSpeciesName("Indian Leopard");
        cub.setCause(RescueCause.ORPHANED);
        cub.setIntakeCondition(HealthStatus.SICK);
        cub.setStatus(RescueStatus.REHABILITATING);
        cub.setRescuedAt(LocalDateTime.of(2026, 7, 27, 10, 0));
        when(rescues.findById(2L)).thenReturn(Optional.of(cub));
        when(treatments.countByRescueId(2L)).thenReturn(1L);
        RehabProgress fit = new RehabProgress();
        fit.setFitForRelease(true);
        when(rehab.findFirstByRescueIdOrderByAssessedOnDescIdDesc(2L)).thenReturn(Optional.of(fit));

        AuthUser officer = new AuthUser(2L, "officer", "Anita Sharma", List.of("FOREST_OFFICER"), 1L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                officer, null, List.of(new SimpleGrantedAuthority("ROLE_FOREST_OFFICER"))));
    }

    @AfterEach
    void clearUser() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void releaseRecordsDetailsAndSendsAnimalBackIntoTheWild() {
        ReleaseInfo release = new ReleaseInfo(LocalDate.of(2026, 10, 9), 1L, 2L, 22.38, 80.57, "Soft release");

        RescueResponse r = service.transition(2L, new RescueTransitionRequest(RescueStatus.RELEASED, null, null, release));

        assertThat(r.status()).isEqualTo(RescueStatus.RELEASED);
        assertThat(r.release().releaseZoneId()).isEqualTo(2L);
        ArgumentCaptor<AnimalStatusUpdate> sent = ArgumentCaptor.forClass(AnimalStatusUpdate.class);
        verify(wildlife).updateStatus(eq(6L), sent.capture());
        assertThat(sent.getValue().status()).isEqualTo("IN_WILD");
        assertThat(sent.getValue().healthStatus()).isEqualTo("HEALTHY");
        assertThat(sent.getValue().zoneId()).isEqualTo(2L);
        assertThat(sent.getValue().sourceReference()).isEqualTo("RESCUE:RSC-2026-0002");
        assertThat(r.warnings()).isEmpty();
    }

    @Test
    void wildlifeDownStillReleasesWithAWarning() {
        when(wildlife.updateStatus(any(), any())).thenThrow(new RuntimeException("Connection refused"));
        ReleaseInfo release = new ReleaseInfo(LocalDate.of(2026, 10, 9), 1L, 2L, 22.38, 80.57, null);

        RescueResponse r = service.transition(2L, new RescueTransitionRequest(RescueStatus.RELEASED, null, null, release));

        assertThat(r.status()).isEqualTo(RescueStatus.RELEASED);
        assertThat(r.warnings()).singleElement().asString().contains("could not be updated in wildlife-service");
    }
}

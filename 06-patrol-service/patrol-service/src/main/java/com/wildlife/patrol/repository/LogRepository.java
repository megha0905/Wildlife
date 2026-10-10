package com.wildlife.patrol.repository;

import com.wildlife.patrol.entity.ObservationType;
import com.wildlife.patrol.entity.PatrolLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LogRepository extends JpaRepository<PatrolLog, Long> {

    List<PatrolLog> findByScheduleIdOrderByLoggedAtAsc(Long scheduleId);

    @EntityGraph(attributePaths = {"schedule", "schedule.route"})
    Optional<PatrolLog> findWithScheduleById(Long id);

    long countByLoggedAtGreaterThanEqualAndLoggedAtLessThan(LocalDateTime from, LocalDateTime to);

    long countByObservationTypeAndLoggedAtGreaterThanEqualAndLoggedAtLessThan(ObservationType type,
                                                                             LocalDateTime from, LocalDateTime to);
}

package com.wildlife.rescue.repository;

import com.wildlife.rescue.entity.RehabProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RehabProgressRepository extends JpaRepository<RehabProgress, Long> {

    List<RehabProgress> findByRescueIdOrderByAssessedOnAscIdAsc(Long rescueId);

    /** Latest assessment decides whether the animal may be released. */
    Optional<RehabProgress> findFirstByRescueIdOrderByAssessedOnDescIdDesc(Long rescueId);
}

package com.wildlife.programme.repository;

import com.wildlife.programme.entity.Activity;
import com.wildlife.programme.entity.ActivityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByProgrammeIdOrderByPlannedStartAscIdAsc(Long programmeId);

    Optional<Activity> findByIdAndProgrammeId(Long id, Long programmeId);

    long countByProgrammeId(Long programmeId);

    long countByProgrammeIdAndStatus(Long programmeId, ActivityStatus status);

    long countByStatusNotAndPlannedEndBefore(ActivityStatus status, LocalDate date);

    @Modifying
    @Query("delete from Activity a where a.programme.id = :programmeId")
    void deleteByProgrammeId(@Param("programmeId") Long programmeId);
}

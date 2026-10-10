package com.wildlife.programme.repository;

import com.wildlife.programme.entity.ProgressUpdate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProgressUpdateRepository extends JpaRepository<ProgressUpdate, Long> {

    List<ProgressUpdate> findByProgrammeIdOrderByUpdateDateDescIdDesc(Long programmeId);

    List<ProgressUpdate> findByProgrammeIdOrderByUpdateDateDescIdDesc(Long programmeId, Pageable pageable);

    boolean existsByTargetId(Long targetId);

    @Modifying
    @Query("delete from ProgressUpdate u where u.programme.id = :programmeId")
    void deleteByProgrammeId(@Param("programmeId") Long programmeId);
}

package com.wildlife.programme.repository;

import com.wildlife.programme.entity.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TargetRepository extends JpaRepository<Target, Long> {

    List<Target> findByProgrammeIdOrderByIdAsc(Long programmeId);

    @Query("select t from Target t where t.programme.id in :ids")
    List<Target> findByProgrammeIds(@Param("ids") Collection<Long> programmeIds);

    Optional<Target> findByIdAndProgrammeId(Long id, Long programmeId);

    @Modifying
    @Query("delete from Target t where t.programme.id = :programmeId")
    void deleteByProgrammeId(@Param("programmeId") Long programmeId);
}

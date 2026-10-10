package com.wildlife.programme.repository;

import com.wildlife.programme.entity.Programme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProgrammeRepository extends JpaRepository<Programme, Long>, JpaSpecificationExecutor<Programme> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    @Query("select p.status, count(p) from Programme p group by p.status")
    List<Object[]> countGroupedByStatus();

    @Query("select p.programmeType, count(p) from Programme p group by p.programmeType")
    List<Object[]> countGroupedByType();
}

package com.wildlife.patrol.repository;

import com.wildlife.patrol.entity.PatrolTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TeamRepository extends JpaRepository<PatrolTeam, Long>, JpaSpecificationExecutor<PatrolTeam> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}

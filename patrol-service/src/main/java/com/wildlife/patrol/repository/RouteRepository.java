package com.wildlife.patrol.repository;

import com.wildlife.patrol.entity.PatrolRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RouteRepository extends JpaRepository<PatrolRoute, Long>, JpaSpecificationExecutor<PatrolRoute> {
}

package com.wildlife.patrol.repository;

import com.wildlife.patrol.entity.PatrolSchedule;
import com.wildlife.patrol.entity.PatrolStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<PatrolSchedule, Long>, JpaSpecificationExecutor<PatrolSchedule> {

    @Override
    @EntityGraph(attributePaths = {"team", "route"})
    Page<PatrolSchedule> findAll(Specification<PatrolSchedule> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"team", "route"})
    Optional<PatrolSchedule> findWithTeamAndRouteById(Long id);

    long countByTeamId(Long teamId);

    long countByRouteId(Long routeId);

    long countByPatrolDate(LocalDate date);

    @Query("select s.status, count(s) from PatrolSchedule s where s.patrolDate between :from and :to group by s.status")
    List<Object[]> countByStatusBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select coalesce(sum(s.route.distanceKm), 0.0) from PatrolSchedule s
            where s.status = :status and s.patrolDate between :from and :to
            """)
    Double sumDistanceBetween(@Param("status") PatrolStatus status, @Param("from") LocalDate from,
                              @Param("to") LocalDate to);
}

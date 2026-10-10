package com.wildlife.incident.repository;

import com.wildlife.incident.entity.IncidentAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<IncidentAssignment, Long> {

    List<IncidentAssignment> findByIncidentIdOrderByAssignedAtAsc(Long incidentId);

    /** Active assignments for many incidents at once (avoids one query per incident). */
    @Query("select a from IncidentAssignment a where a.incident.id in :ids and a.active = true order by a.assignedAt")
    List<IncidentAssignment> findActiveForIncidents(@Param("ids") Collection<Long> incidentIds);

    long countByIncidentIdAndActiveTrue(Long incidentId);

    boolean existsByIncidentIdAndStaffIdAndActiveTrue(Long incidentId, Long staffId);

    Optional<IncidentAssignment> findByIdAndIncidentId(Long id, Long incidentId);

    @Modifying
    @Query("delete from IncidentAssignment a where a.incident.id = :incidentId")
    void deleteByIncidentId(@Param("incidentId") Long incidentId);
}

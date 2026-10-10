package com.wildlife.incident.repository;

import com.wildlife.incident.entity.IncidentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HistoryRepository extends JpaRepository<IncidentStatusHistory, Long> {

    List<IncidentStatusHistory> findByIncidentIdOrderByChangedAtAscIdAsc(Long incidentId);

    @Modifying
    @Query("delete from IncidentStatusHistory h where h.incident.id = :incidentId")
    void deleteByIncidentId(@Param("incidentId") Long incidentId);
}

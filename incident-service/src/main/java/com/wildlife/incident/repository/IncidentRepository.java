package com.wildlife.incident.repository;

import com.wildlife.incident.entity.Incident;
import com.wildlife.incident.entity.IncidentStatus;
import com.wildlife.incident.entity.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    /** Next number for INC-<year>-<nnnn> codes (PostgreSQL sequence created in V1__init.sql). */
    @Query(value = "select nextval('incident_code_seq')", nativeQuery = true)
    Long nextCodeNumber();

    /** All incidents in one zone whose occurredAt falls in [from, to), newest first. */
    @Query("""
            select i from Incident i
            where i.zoneId = :zoneId and i.occurredAt >= :from and i.occurredAt < :to
            order by i.occurredAt desc, i.id desc
            """)
    List<Incident> findInZoneBetween(@Param("zoneId") Long zoneId,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to);

    @Query("select i.status, count(i) from Incident i where i.occurredAt >= :from and i.occurredAt < :to group by i.status")
    List<Object[]> countByStatusBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select i.incidentType, count(i) from Incident i where i.occurredAt >= :from and i.occurredAt < :to group by i.incidentType")
    List<Object[]> countByTypeBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select i.severity, count(i) from Incident i where i.occurredAt >= :from and i.occurredAt < :to group by i.severity")
    List<Object[]> countBySeverityBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countByStatusIn(Collection<IncidentStatus> statuses);

    long countBySeverityAndStatusIn(Severity severity, Collection<IncidentStatus> statuses);
}

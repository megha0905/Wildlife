package com.wildlife.rescue.repository;

import com.wildlife.rescue.entity.RescueCase;
import com.wildlife.rescue.entity.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface RescueRepository extends JpaRepository<RescueCase, Long>, JpaSpecificationExecutor<RescueCase> {

    @Query(value = "select nextval('rescue_case_seq')", nativeQuery = true)
    Long nextCaseNumber();

    @Query("select r.status, count(r) from RescueCase r group by r.status")
    List<Object[]> countGroupedByStatus();

    @Query("select r.cause, count(r) from RescueCase r group by r.cause")
    List<Object[]> countGroupedByCause();

    long countByStatusIn(Collection<RescueStatus> statuses);

    long countByStatusAndReleaseDateBetween(RescueStatus status, LocalDate from, LocalDate to);

    long countByStatusAndTransferDateBetween(RescueStatus status, LocalDate from, LocalDate to);

    @Query("select count(r) from RescueCase r where r.rescuedAt >= :from and r.rescuedAt < :to")
    long countRescuedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}

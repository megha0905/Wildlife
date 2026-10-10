package com.wildlife.resource.repository;

import com.wildlife.resource.entity.Allocation;
import com.wildlife.resource.entity.AllocationStatus;
import com.wildlife.resource.entity.ResourceKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AllocationRepository extends JpaRepository<Allocation, Long>, JpaSpecificationExecutor<Allocation> {

    /**
     * Allocations of the same resource whose window overlaps [start, end).
     * Overlap rule (CONTRACTS.md 6.4): existing.start < new.end AND new.start < existing.end.
     * Pass excludeId = -1 when there is nothing to exclude (create).
     */
    @Query("""
            select a from Allocation a
            where a.resourceKind = :kind
              and a.resourceId = :resourceId
              and a.status in :statuses
              and a.startAt < :endAt
              and :startAt < a.endAt
              and a.id <> :excludeId
            order by a.startAt
            """)
    List<Allocation> findOverlapping(@Param("kind") ResourceKind kind,
                                     @Param("resourceId") Long resourceId,
                                     @Param("startAt") LocalDateTime startAt,
                                     @Param("endAt") LocalDateTime endAt,
                                     @Param("statuses") Collection<AllocationStatus> statuses,
                                     @Param("excludeId") Long excludeId);

    boolean existsByResourceKindAndResourceId(ResourceKind kind, Long resourceId);

    long countByStatus(AllocationStatus status);

    @Query("""
            select count(a) from Allocation a
            where a.status in :statuses and a.startAt < :dayEnd and :dayStart < a.endAt
            """)
    long countOverlappingDay(@Param("dayStart") LocalDateTime dayStart,
                             @Param("dayEnd") LocalDateTime dayEnd,
                             @Param("statuses") Collection<AllocationStatus> statuses);
}

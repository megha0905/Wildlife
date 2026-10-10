package com.wildlife.habitat.repository;

import com.wildlife.habitat.entity.WaterResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WaterResourceRepository extends JpaRepository<WaterResource, Long>, JpaSpecificationExecutor<WaterResource> {

    @Override
    @EntityGraph(attributePaths = {"zone", "zone.area"})
    Page<WaterResource> findAll(Specification<WaterResource> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"zone", "zone.area"})
    Optional<WaterResource> findWithZoneById(Long id);

    long countByZoneId(Long zoneId);

    /** Rows of [WaterStatus, count]. */
    @Query("select w.status, count(w) from WaterResource w group by w.status")
    List<Object[]> countGroupedByStatus();
}

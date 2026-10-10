package com.wildlife.habitat.repository;

import com.wildlife.habitat.entity.Habitat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HabitatRepository extends JpaRepository<Habitat, Long>, JpaSpecificationExecutor<Habitat> {

    @Override
    @EntityGraph(attributePaths = {"zone", "zone.area"})
    Page<Habitat> findAll(Specification<Habitat> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"zone", "zone.area"})
    Optional<Habitat> findWithZoneById(Long id);

    long countByZoneId(Long zoneId);

    /** Rows of [HabitatCondition, count]. */
    @Query("select h.condition, count(h) from Habitat h group by h.condition")
    List<Object[]> countGroupedByCondition();
}

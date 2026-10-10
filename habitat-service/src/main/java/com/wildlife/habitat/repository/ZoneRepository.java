package com.wildlife.habitat.repository;

import com.wildlife.habitat.entity.Zone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ZoneRepository extends JpaRepository<Zone, Long>, JpaSpecificationExecutor<Zone> {

    @Override
    @EntityGraph(attributePaths = "area")
    Page<Zone> findAll(Specification<Zone> spec, Pageable pageable);

    @EntityGraph(attributePaths = "area")
    Optional<Zone> findWithAreaById(Long id);

    @EntityGraph(attributePaths = "area")
    List<Zone> findByAreaIdOrderByNameAsc(Long areaId);

    long countByAreaId(Long areaId);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    /** Rows of [areaId, zoneCount]. */
    @Query("select z.area.id, count(z) from Zone z group by z.area.id")
    List<Object[]> countGroupedByArea();
}

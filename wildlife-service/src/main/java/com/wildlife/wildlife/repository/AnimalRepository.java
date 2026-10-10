package com.wildlife.wildlife.repository;

import com.wildlife.wildlife.entity.Animal;
import com.wildlife.wildlife.entity.AnimalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AnimalRepository extends JpaRepository<Animal, Long>, JpaSpecificationExecutor<Animal> {

    @Override
    @EntityGraph(attributePaths = "species")
    Page<Animal> findAll(Specification<Animal> spec, Pageable pageable);

    @EntityGraph(attributePaths = "species")
    Optional<Animal> findWithSpeciesById(Long id);

    @EntityGraph(attributePaths = "species")
    Optional<Animal> findByTagIdIgnoreCase(String tagId);

    boolean existsByTagIdIgnoreCase(String tagId);

    boolean existsByTagIdIgnoreCaseAndIdNot(String tagId, Long id);

    long countBySpeciesId(Long speciesId);

    /** Rows of [speciesId, count]. */
    @Query("select a.species.id, count(a) from Animal a group by a.species.id")
    List<Object[]> countGroupedBySpecies();

    @Query("select a.healthStatus, count(a) from Animal a group by a.healthStatus")
    List<Object[]> countGroupedByHealth();

    @Query("select a.status, count(a) from Animal a group by a.status")
    List<Object[]> countGroupedByStatus();

    @Query("select a.species.conservationStatus, count(a) from Animal a group by a.species.conservationStatus")
    List<Object[]> countGroupedByConservationStatus();

    @Query("select count(a) from Animal a where a.status = :status and (a.lastSeenAt is null or a.lastSeenAt < :before)")
    long countNotSeenSince(@Param("status") AnimalStatus status, @Param("before") LocalDateTime before);
}

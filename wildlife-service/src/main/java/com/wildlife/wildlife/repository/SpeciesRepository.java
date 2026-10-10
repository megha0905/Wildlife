package com.wildlife.wildlife.repository;

import com.wildlife.wildlife.entity.Species;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SpeciesRepository extends JpaRepository<Species, Long>, JpaSpecificationExecutor<Species> {

    boolean existsByCommonNameIgnoreCase(String commonName);

    boolean existsByScientificNameIgnoreCase(String scientificName);

    boolean existsByCommonNameIgnoreCaseAndIdNot(String commonName, Long id);

    boolean existsByScientificNameIgnoreCaseAndIdNot(String scientificName, Long id);
}

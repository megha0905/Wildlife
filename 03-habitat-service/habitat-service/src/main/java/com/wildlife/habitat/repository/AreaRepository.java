package com.wildlife.habitat.repository;

import com.wildlife.habitat.entity.ProtectedArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface AreaRepository extends JpaRepository<ProtectedArea, Long>, JpaSpecificationExecutor<ProtectedArea> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    @Query("select coalesce(sum(a.totalAreaSqKm), 0.0) from ProtectedArea a")
    Double sumTotalAreaSqKm();
}

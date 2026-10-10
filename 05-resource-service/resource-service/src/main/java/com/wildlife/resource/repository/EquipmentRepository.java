package com.wildlife.resource.repository;

import com.wildlife.resource.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long>, JpaSpecificationExecutor<Equipment> {

    boolean existsByAssetTagIgnoreCase(String assetTag);

    boolean existsByAssetTagIgnoreCaseAndIdNot(String assetTag, Long id);

    @Query("select e.status, count(e) from Equipment e group by e.status")
    List<Object[]> countGroupedByStatus();
}

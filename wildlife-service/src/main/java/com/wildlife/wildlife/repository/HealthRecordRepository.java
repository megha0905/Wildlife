package com.wildlife.wildlife.repository;

import com.wildlife.wildlife.entity.HealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {

    List<HealthRecord> findByAnimalIdOrderByRecordDateDescIdDesc(Long animalId);

    @Query("select max(h.recordDate) from HealthRecord h where h.animal.id = :animalId")
    Optional<LocalDate> findLatestRecordDate(@Param("animalId") Long animalId);

    @Modifying
    @Query("delete from HealthRecord h where h.animal.id = :animalId")
    void deleteByAnimalId(@Param("animalId") Long animalId);
}

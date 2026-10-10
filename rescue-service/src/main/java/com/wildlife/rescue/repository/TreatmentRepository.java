package com.wildlife.rescue.repository;

import com.wildlife.rescue.entity.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TreatmentRepository extends JpaRepository<Treatment, Long> {

    List<Treatment> findByRescueIdOrderByTreatmentDateAscIdAsc(Long rescueId);

    long countByRescueId(Long rescueId);
}

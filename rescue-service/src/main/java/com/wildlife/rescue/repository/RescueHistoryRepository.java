package com.wildlife.rescue.repository;

import com.wildlife.rescue.entity.RescueStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RescueHistoryRepository extends JpaRepository<RescueStatusHistory, Long> {

    List<RescueStatusHistory> findByRescueIdOrderByChangedAtAscIdAsc(Long rescueId);
}

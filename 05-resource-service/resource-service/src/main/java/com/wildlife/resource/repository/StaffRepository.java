package com.wildlife.resource.repository;

import com.wildlife.resource.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface StaffRepository extends JpaRepository<Staff, Long>, JpaSpecificationExecutor<Staff> {

    boolean existsByEmployeeCodeIgnoreCase(String code);

    boolean existsByEmployeeCodeIgnoreCaseAndIdNot(String code, Long id);

    List<Staff> findByIdIn(Collection<Long> ids);

    @Query("select s.status, count(s) from Staff s group by s.status")
    List<Object[]> countGroupedByStatus();

    @Query("select s.designation, count(s) from Staff s group by s.designation")
    List<Object[]> countGroupedByDesignation();
}

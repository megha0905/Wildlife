package com.wildlife.resource.service;

import com.wildlife.resource.dto.ResourceSummary;
import com.wildlife.resource.entity.AllocationStatus;
import com.wildlife.resource.entity.Designation;
import com.wildlife.resource.entity.ResourceStatus;
import com.wildlife.resource.entity.StaffStatus;
import com.wildlife.resource.repository.AllocationRepository;
import com.wildlife.resource.repository.EquipmentRepository;
import com.wildlife.resource.repository.StaffRepository;
import com.wildlife.resource.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ResourceSummaryService {

    private final StaffRepository staffRepository;
    private final VehicleRepository vehicleRepository;
    private final EquipmentRepository equipmentRepository;
    private final AllocationRepository allocationRepository;
    private final Clock clock;

    public ResourceSummaryService(StaffRepository staffRepository, VehicleRepository vehicleRepository,
                                  EquipmentRepository equipmentRepository, AllocationRepository allocationRepository,
                                  Clock clock) {
        this.staffRepository = staffRepository;
        this.vehicleRepository = vehicleRepository;
        this.equipmentRepository = equipmentRepository;
        this.allocationRepository = allocationRepository;
        this.clock = clock;
    }

    public ResourceSummary summary() {
        LocalDate today = LocalDate.now(clock);
        return new ResourceSummary(
                staffRepository.count(),
                zeroFilled(StaffStatus.values(), staffRepository.countGroupedByStatus()),
                zeroFilled(Designation.values(), staffRepository.countGroupedByDesignation()),
                zeroFilled(ResourceStatus.values(), vehicleRepository.countGroupedByStatus()),
                zeroFilled(ResourceStatus.values(), equipmentRepository.countGroupedByStatus()),
                allocationRepository.countByStatus(AllocationStatus.ACTIVE),
                allocationRepository.countOverlappingDay(today.atStartOfDay(), today.plusDays(1).atStartOfDay(),
                        AllocationService.HOLDING));
    }

    static <E extends Enum<E>> Map<String, Long> zeroFilled(E[] values, List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (E e : values) map.put(e.name(), 0L);
        for (Object[] row : rows) map.put(((Enum<?>) row[0]).name(), (Long) row[1]);
        return map;
    }
}

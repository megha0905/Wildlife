package com.wildlife.resource.service;

import com.wildlife.resource.dto.AllocationResponse;
import com.wildlife.resource.dto.EquipmentResponse;
import com.wildlife.resource.dto.StaffRef;
import com.wildlife.resource.dto.StaffResponse;
import com.wildlife.resource.dto.VehicleResponse;
import com.wildlife.resource.entity.Allocation;
import com.wildlife.resource.entity.Equipment;
import com.wildlife.resource.entity.Staff;
import com.wildlife.resource.entity.Vehicle;

final class ResourceMapper {

    private ResourceMapper() {
    }

    static StaffResponse toStaff(Staff s) {
        return new StaffResponse(s.getId(), s.getEmployeeCode(), s.getFullName(), s.getDesignation(), s.getStatus(),
                s.getPhone(), s.getEmail(), s.getHomeAreaId(), s.getJoinedOn());
    }

    static StaffRef toStaffRef(Staff s) {
        return new StaffRef(s.getId(), s.getEmployeeCode(), s.getFullName(), s.getDesignation(), s.getStatus());
    }

    static VehicleResponse toVehicle(Vehicle v) {
        return new VehicleResponse(v.getId(), v.getRegistrationNumber(), v.getVehicleType(), v.getStatus(),
                v.getMake(), v.getModel(), v.getSeatingCapacity(), v.getHomeAreaId(), v.getLastServicedOn());
    }

    static EquipmentResponse toEquipment(Equipment e) {
        return new EquipmentResponse(e.getId(), e.getAssetTag(), e.getName(), e.getEquipmentType(), e.getStatus(),
                e.getHomeAreaId(), e.getPurchasedOn(), e.getNotes());
    }

    static AllocationResponse toAllocation(Allocation a) {
        return new AllocationResponse(a.getId(), a.getResourceKind(), a.getResourceId(), a.getResourceLabel(),
                a.getPurpose(), a.getReferenceId(), a.getReferenceLabel(), a.getStartAt(), a.getEndAt(),
                a.getStatus(), a.getAllocatedByUserId(), a.getAllocatedByName(), a.getNotes(), a.getCreatedAt());
    }

    static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

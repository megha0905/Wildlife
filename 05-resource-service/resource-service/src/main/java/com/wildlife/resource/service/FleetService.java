package com.wildlife.resource.service;

import com.wildlife.resource.dto.EquipmentRequest;
import com.wildlife.resource.dto.EquipmentResponse;
import com.wildlife.resource.dto.PageResponse;
import com.wildlife.resource.dto.VehicleRequest;
import com.wildlife.resource.dto.VehicleResponse;
import com.wildlife.resource.entity.Equipment;
import com.wildlife.resource.entity.EquipmentType;
import com.wildlife.resource.entity.ResourceKind;
import com.wildlife.resource.entity.ResourceStatus;
import com.wildlife.resource.entity.Vehicle;
import com.wildlife.resource.entity.VehicleType;
import com.wildlife.resource.exception.ConflictException;
import com.wildlife.resource.exception.ResourceNotFoundException;
import com.wildlife.resource.repository.AllocationRepository;
import com.wildlife.resource.repository.EquipmentRepository;
import com.wildlife.resource.repository.VehicleRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

import static com.wildlife.resource.service.ResourceMapper.trimToNull;

/** Vehicles and equipment. */
@Service
@Transactional
public class FleetService {

    private final VehicleRepository vehicleRepository;
    private final EquipmentRepository equipmentRepository;
    private final AllocationRepository allocationRepository;

    public FleetService(VehicleRepository vehicleRepository, EquipmentRepository equipmentRepository,
                        AllocationRepository allocationRepository) {
        this.vehicleRepository = vehicleRepository;
        this.equipmentRepository = equipmentRepository;
        this.allocationRepository = allocationRepository;
    }

    // ---------------------------------------------------------------- vehicles

    @Transactional(readOnly = true)
    public PageResponse<VehicleResponse> searchVehicles(VehicleType type, ResourceStatus status, Long areaId,
                                                        Pageable pageable) {
        Specification<Vehicle> spec = Specification.where(null);
        if (type != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("vehicleType"), type));
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (areaId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("homeAreaId"), areaId));
        return PageResponse.of(vehicleRepository.findAll(spec, pageable), ResourceMapper::toVehicle);
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicle(Long id) {
        return ResourceMapper.toVehicle(findVehicle(id));
    }

    public VehicleResponse createVehicle(VehicleRequest req) {
        String reg = normaliseRegistration(req.registrationNumber());
        if (vehicleRepository.existsByRegistrationNumberIgnoreCase(reg)) {
            throw new ConflictException("Vehicle " + reg + " is already registered");
        }
        Vehicle v = new Vehicle();
        applyVehicle(v, req, reg);
        return ResourceMapper.toVehicle(vehicleRepository.save(v));
    }

    public VehicleResponse updateVehicle(Long id, VehicleRequest req) {
        Vehicle v = findVehicle(id);
        String reg = normaliseRegistration(req.registrationNumber());
        if (vehicleRepository.existsByRegistrationNumberIgnoreCaseAndIdNot(reg, id)) {
            throw new ConflictException("Vehicle " + reg + " is already registered");
        }
        applyVehicle(v, req, reg);
        return ResourceMapper.toVehicle(v);
    }

    public void deleteVehicle(Long id) {
        Vehicle v = findVehicle(id);
        if (allocationRepository.existsByResourceKindAndResourceId(ResourceKind.VEHICLE, id)) {
            throw new ConflictException("Vehicle " + v.getRegistrationNumber() + " has allocations; retire it instead");
        }
        vehicleRepository.delete(v);
    }

    Vehicle findVehicle(Long id) {
        return vehicleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
    }

    // ---------------------------------------------------------------- equipment

    @Transactional(readOnly = true)
    public PageResponse<EquipmentResponse> searchEquipment(EquipmentType type, ResourceStatus status, Long areaId,
                                                           Pageable pageable) {
        Specification<Equipment> spec = Specification.where(null);
        if (type != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("equipmentType"), type));
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (areaId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("homeAreaId"), areaId));
        return PageResponse.of(equipmentRepository.findAll(spec, pageable), ResourceMapper::toEquipment);
    }

    @Transactional(readOnly = true)
    public EquipmentResponse getEquipment(Long id) {
        return ResourceMapper.toEquipment(findEquipment(id));
    }

    public EquipmentResponse createEquipment(EquipmentRequest req) {
        String tag = req.assetTag().trim().toUpperCase(Locale.ROOT);
        if (equipmentRepository.existsByAssetTagIgnoreCase(tag)) {
            throw new ConflictException("Asset tag " + tag + " is already in use");
        }
        Equipment e = new Equipment();
        applyEquipment(e, req, tag);
        return ResourceMapper.toEquipment(equipmentRepository.save(e));
    }

    public EquipmentResponse updateEquipment(Long id, EquipmentRequest req) {
        Equipment e = findEquipment(id);
        String tag = req.assetTag().trim().toUpperCase(Locale.ROOT);
        if (equipmentRepository.existsByAssetTagIgnoreCaseAndIdNot(tag, id)) {
            throw new ConflictException("Asset tag " + tag + " is already in use");
        }
        applyEquipment(e, req, tag);
        return ResourceMapper.toEquipment(e);
    }

    public void deleteEquipment(Long id) {
        Equipment e = findEquipment(id);
        if (allocationRepository.existsByResourceKindAndResourceId(ResourceKind.EQUIPMENT, id)) {
            throw new ConflictException("Equipment " + e.getAssetTag() + " has allocations; retire it instead");
        }
        equipmentRepository.delete(e);
    }

    Equipment findEquipment(Long id) {
        return equipmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Equipment", id));
    }

    // ---------------------------------------------------------------- helpers

    private static String normaliseRegistration(String reg) {
        return reg.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "-");
    }

    private static void applyVehicle(Vehicle v, VehicleRequest req, String reg) {
        v.setRegistrationNumber(reg);
        v.setVehicleType(req.vehicleType());
        v.setStatus(req.status());
        v.setMake(trimToNull(req.make()));
        v.setModel(trimToNull(req.model()));
        v.setSeatingCapacity(req.seatingCapacity());
        v.setHomeAreaId(req.homeAreaId());
        v.setLastServicedOn(req.lastServicedOn());
    }

    private static void applyEquipment(Equipment e, EquipmentRequest req, String tag) {
        e.setAssetTag(tag);
        e.setName(req.name().trim());
        e.setEquipmentType(req.equipmentType());
        e.setStatus(req.status());
        e.setHomeAreaId(req.homeAreaId());
        e.setPurchasedOn(req.purchasedOn());
        e.setNotes(trimToNull(req.notes()));
    }
}

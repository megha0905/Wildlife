package com.wildlife.resource.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Reserves one staff member, vehicle or equipment item for [startAt, endAt).
 * resourceId points into this service's staff/vehicles/equipment table depending on resourceKind;
 * referenceId points to the record in another service (patrol schedule, incident, rescue case...).
 */
@Entity
@Table(name = "allocations")
public class Allocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_kind", nullable = false, length = 20)
    private ResourceKind resourceKind;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    /** Snapshot: staff name, vehicle registration or asset tag + name. */
    @Column(name = "resource_label", nullable = false, length = 160)
    private String resourceLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AllocationPurpose purpose;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_label", length = 200)
    private String referenceLabel;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AllocationStatus status;

    @Column(name = "allocated_by_user_id")
    private Long allocatedByUserId;

    @Column(name = "allocated_by_name", length = 120)
    private String allocatedByName;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ResourceKind getResourceKind() { return resourceKind; }
    public void setResourceKind(ResourceKind resourceKind) { this.resourceKind = resourceKind; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public String getResourceLabel() { return resourceLabel; }
    public void setResourceLabel(String resourceLabel) { this.resourceLabel = resourceLabel; }
    public AllocationPurpose getPurpose() { return purpose; }
    public void setPurpose(AllocationPurpose purpose) { this.purpose = purpose; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public String getReferenceLabel() { return referenceLabel; }
    public void setReferenceLabel(String referenceLabel) { this.referenceLabel = referenceLabel; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public AllocationStatus getStatus() { return status; }
    public void setStatus(AllocationStatus status) { this.status = status; }
    public Long getAllocatedByUserId() { return allocatedByUserId; }
    public void setAllocatedByUserId(Long allocatedByUserId) { this.allocatedByUserId = allocatedByUserId; }
    public String getAllocatedByName() { return allocatedByName; }
    public void setAllocatedByName(String allocatedByName) { this.allocatedByName = allocatedByName; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

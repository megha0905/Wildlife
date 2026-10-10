package com.wildlife.habitat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "water_resources")
public class WaterResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private WaterSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WaterStatus status;

    @Column(nullable = false)
    private boolean perennial;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "capacity_cubic_m")
    private Double capacityCubicM;

    @Column(name = "last_inspected_at")
    private LocalDateTime lastInspectedAt;

    @Column(length = 2000)
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public WaterSourceType getSourceType() { return sourceType; }
    public void setSourceType(WaterSourceType sourceType) { this.sourceType = sourceType; }
    public WaterStatus getStatus() { return status; }
    public void setStatus(WaterStatus status) { this.status = status; }
    public boolean isPerennial() { return perennial; }
    public void setPerennial(boolean perennial) { this.perennial = perennial; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getCapacityCubicM() { return capacityCubicM; }
    public void setCapacityCubicM(Double capacityCubicM) { this.capacityCubicM = capacityCubicM; }
    public LocalDateTime getLastInspectedAt() { return lastInspectedAt; }
    public void setLastInspectedAt(LocalDateTime lastInspectedAt) { this.lastInspectedAt = lastInspectedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

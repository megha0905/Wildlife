package com.wildlife.wildlife.entity;

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

/** An individually identified animal. areaId/zoneId point to habitat-service (IDs only). */
@Entity
@Table(name = "animals")
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tag_id", nullable = false, unique = true, length = 30)
    private String tagId;

    @Column(length = 80)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "species_id", nullable = false)
    private Species species;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Sex sex;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "birth_year_estimated", nullable = false)
    private boolean birthYearEstimated = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 20)
    private HealthStatus healthStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnimalStatus status;

    @Column(name = "identification_marks", length = 1000)
    private String identificationMarks;

    @Column(name = "area_id")
    private Long areaId;

    @Column(name = "zone_id")
    private Long zoneId;

    /** Snapshot of the zone name from habitat-service, filled when zoneId is saved. */
    @Column(name = "zone_name", length = 150)
    private String zoneName;

    @Column(name = "last_seen_latitude")
    private Double lastSeenLatitude;

    @Column(name = "last_seen_longitude")
    private Double lastSeenLongitude;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(length = 2000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTagId() { return tagId; }
    public void setTagId(String tagId) { this.tagId = tagId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Species getSpecies() { return species; }
    public void setSpecies(Species species) { this.species = species; }
    public Sex getSex() { return sex; }
    public void setSex(Sex sex) { this.sex = sex; }
    public Integer getBirthYear() { return birthYear; }
    public void setBirthYear(Integer birthYear) { this.birthYear = birthYear; }
    public boolean isBirthYearEstimated() { return birthYearEstimated; }
    public void setBirthYearEstimated(boolean birthYearEstimated) { this.birthYearEstimated = birthYearEstimated; }
    public HealthStatus getHealthStatus() { return healthStatus; }
    public void setHealthStatus(HealthStatus healthStatus) { this.healthStatus = healthStatus; }
    public AnimalStatus getStatus() { return status; }
    public void setStatus(AnimalStatus status) { this.status = status; }
    public String getIdentificationMarks() { return identificationMarks; }
    public void setIdentificationMarks(String identificationMarks) { this.identificationMarks = identificationMarks; }
    public Long getAreaId() { return areaId; }
    public void setAreaId(Long areaId) { this.areaId = areaId; }
    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    public Double getLastSeenLatitude() { return lastSeenLatitude; }
    public void setLastSeenLatitude(Double lastSeenLatitude) { this.lastSeenLatitude = lastSeenLatitude; }
    public Double getLastSeenLongitude() { return lastSeenLongitude; }
    public void setLastSeenLongitude(Double lastSeenLongitude) { this.lastSeenLongitude = lastSeenLongitude; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

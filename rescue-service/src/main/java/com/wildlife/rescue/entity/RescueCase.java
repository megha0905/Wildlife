package com.wildlife.rescue.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A rescue case. animalId/speciesId (wildlife), areaId/zoneId (habitat), incidentId (incident) are ids only. */
@Entity
@Table(name = "rescue_cases")
public class RescueCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** RSC-<year>-<4-digit sequence>. */
    @Column(name = "case_number", nullable = false, unique = true, length = 20)
    private String caseNumber;

    @Column(name = "animal_id")
    private Long animalId;

    @Column(name = "animal_tag_id", length = 30)
    private String animalTagId;

    @Column(name = "species_id")
    private Long speciesId;

    @Column(name = "species_name", nullable = false, length = 120)
    private String speciesName;

    @Column(name = "animal_description", length = 500)
    private String animalDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RescueCause cause;

    @Enumerated(EnumType.STRING)
    @Column(name = "intake_condition", nullable = false, length = 20)
    private HealthStatus intakeCondition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RescueStatus status;

    @Column(name = "rescued_at", nullable = false)
    private LocalDateTime rescuedAt;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "area_id")
    private Long areaId;

    @Column(name = "zone_id")
    private Long zoneId;

    @Column(name = "incident_id")
    private Long incidentId;

    @Column(name = "facility_name", nullable = false, length = 150)
    private String facilityName;

    @Column(name = "registered_by_user_id")
    private Long registeredByUserId;

    @Column(name = "registered_by_name", length = 120)
    private String registeredByName;

    @Column(length = 2000)
    private String notes;

    // ---- transfer (set when the case moves to TRANSFERRED)
    @Column(name = "transfer_destination", length = 150)
    private String transferDestination;

    @Column(name = "transfer_reason", length = 500)
    private String transferReason;

    @Column(name = "transfer_date")
    private LocalDate transferDate;

    // ---- release (set when the case moves to RELEASED)
    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "release_area_id")
    private Long releaseAreaId;

    @Column(name = "release_zone_id")
    private Long releaseZoneId;

    @Column(name = "release_latitude")
    private Double releaseLatitude;

    @Column(name = "release_longitude")
    private Double releaseLongitude;

    @Column(name = "release_notes", length = 1000)
    private String releaseNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }
    public Long getAnimalId() { return animalId; }
    public void setAnimalId(Long animalId) { this.animalId = animalId; }
    public String getAnimalTagId() { return animalTagId; }
    public void setAnimalTagId(String animalTagId) { this.animalTagId = animalTagId; }
    public Long getSpeciesId() { return speciesId; }
    public void setSpeciesId(Long speciesId) { this.speciesId = speciesId; }
    public String getSpeciesName() { return speciesName; }
    public void setSpeciesName(String speciesName) { this.speciesName = speciesName; }
    public String getAnimalDescription() { return animalDescription; }
    public void setAnimalDescription(String animalDescription) { this.animalDescription = animalDescription; }
    public RescueCause getCause() { return cause; }
    public void setCause(RescueCause cause) { this.cause = cause; }
    public HealthStatus getIntakeCondition() { return intakeCondition; }
    public void setIntakeCondition(HealthStatus intakeCondition) { this.intakeCondition = intakeCondition; }
    public RescueStatus getStatus() { return status; }
    public void setStatus(RescueStatus status) { this.status = status; }
    public LocalDateTime getRescuedAt() { return rescuedAt; }
    public void setRescuedAt(LocalDateTime rescuedAt) { this.rescuedAt = rescuedAt; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Long getAreaId() { return areaId; }
    public void setAreaId(Long areaId) { this.areaId = areaId; }
    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }
    public String getFacilityName() { return facilityName; }
    public void setFacilityName(String facilityName) { this.facilityName = facilityName; }
    public Long getRegisteredByUserId() { return registeredByUserId; }
    public void setRegisteredByUserId(Long registeredByUserId) { this.registeredByUserId = registeredByUserId; }
    public String getRegisteredByName() { return registeredByName; }
    public void setRegisteredByName(String registeredByName) { this.registeredByName = registeredByName; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getTransferDestination() { return transferDestination; }
    public void setTransferDestination(String transferDestination) { this.transferDestination = transferDestination; }
    public String getTransferReason() { return transferReason; }
    public void setTransferReason(String transferReason) { this.transferReason = transferReason; }
    public LocalDate getTransferDate() { return transferDate; }
    public void setTransferDate(LocalDate transferDate) { this.transferDate = transferDate; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
    public Long getReleaseAreaId() { return releaseAreaId; }
    public void setReleaseAreaId(Long releaseAreaId) { this.releaseAreaId = releaseAreaId; }
    public Long getReleaseZoneId() { return releaseZoneId; }
    public void setReleaseZoneId(Long releaseZoneId) { this.releaseZoneId = releaseZoneId; }
    public Double getReleaseLatitude() { return releaseLatitude; }
    public void setReleaseLatitude(Double releaseLatitude) { this.releaseLatitude = releaseLatitude; }
    public Double getReleaseLongitude() { return releaseLongitude; }
    public void setReleaseLongitude(Double releaseLongitude) { this.releaseLongitude = releaseLongitude; }
    public String getReleaseNotes() { return releaseNotes; }
    public void setReleaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

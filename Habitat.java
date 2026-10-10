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

import java.time.LocalDate;

@Entity
@Table(name = "habitats")
public class Habitat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "habitat_type", nullable = false, length = 30)
    private HabitatType habitatType;

    @Enumerated(EnumType.STRING)
    @Column(name = "habitat_condition", nullable = false, length = 20)
    private HabitatCondition condition;

    @Column(name = "area_sq_km")
    private Double areaSqKm;

    private Double latitude;
    private Double longitude;

    @Column(name = "dominant_vegetation", length = 300)
    private String dominantVegetation;

    @Column(name = "last_survey_date")
    private LocalDate lastSurveyDate;

    @Column(length = 2000)
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public HabitatType getHabitatType() { return habitatType; }
    public void setHabitatType(HabitatType habitatType) { this.habitatType = habitatType; }
    public HabitatCondition getCondition() { return condition; }
    public void setCondition(HabitatCondition condition) { this.condition = condition; }
    public Double getAreaSqKm() { return areaSqKm; }
    public void setAreaSqKm(Double areaSqKm) { this.areaSqKm = areaSqKm; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getDominantVegetation() { return dominantVegetation; }
    public void setDominantVegetation(String dominantVegetation) { this.dominantVegetation = dominantVegetation; }
    public LocalDate getLastSurveyDate() { return lastSurveyDate; }
    public void setLastSurveyDate(LocalDate lastSurveyDate) { this.lastSurveyDate = lastSurveyDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

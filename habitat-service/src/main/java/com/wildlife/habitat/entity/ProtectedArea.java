package com.wildlife.habitat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A national park, sanctuary or reserve. */
@Entity
@Table(name = "areas")
public class ProtectedArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "area_type", nullable = false, length = 30)
    private AreaType areaType;

    @Column(nullable = false, length = 80)
    private String state;

    @Column(length = 80)
    private String district;

    @Column(name = "total_area_sq_km", nullable = false)
    private Double totalAreaSqKm;

    @Column(name = "established_year")
    private Integer establishedYear;

    @Column(name = "center_latitude", nullable = false)
    private Double centerLatitude;

    @Column(name = "center_longitude", nullable = false)
    private Double centerLongitude;

    @Column(length = 2000)
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public AreaType getAreaType() { return areaType; }
    public void setAreaType(AreaType areaType) { this.areaType = areaType; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public Double getTotalAreaSqKm() { return totalAreaSqKm; }
    public void setTotalAreaSqKm(Double totalAreaSqKm) { this.totalAreaSqKm = totalAreaSqKm; }
    public Integer getEstablishedYear() { return establishedYear; }
    public void setEstablishedYear(Integer establishedYear) { this.establishedYear = establishedYear; }
    public Double getCenterLatitude() { return centerLatitude; }
    public void setCenterLatitude(Double centerLatitude) { this.centerLatitude = centerLatitude; }
    public Double getCenterLongitude() { return centerLongitude; }
    public void setCenterLongitude(Double centerLongitude) { this.centerLongitude = centerLongitude; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

package com.wildlife.wildlife.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "species")
public class Species {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "common_name", nullable = false, unique = true, length = 120)
    private String commonName;

    @Column(name = "scientific_name", nullable = false, unique = true, length = 160)
    private String scientificName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SpeciesCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "conservation_status", nullable = false, length = 30)
    private ConservationStatus conservationStatus;

    /** Legal protection, e.g. "Schedule I" of the Wildlife (Protection) Act. */
    @Column(name = "protection_schedule", length = 60)
    private String protectionSchedule;

    @Column(length = 2000)
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCommonName() { return commonName; }
    public void setCommonName(String commonName) { this.commonName = commonName; }
    public String getScientificName() { return scientificName; }
    public void setScientificName(String scientificName) { this.scientificName = scientificName; }
    public SpeciesCategory getCategory() { return category; }
    public void setCategory(SpeciesCategory category) { this.category = category; }
    public ConservationStatus getConservationStatus() { return conservationStatus; }
    public void setConservationStatus(ConservationStatus conservationStatus) { this.conservationStatus = conservationStatus; }
    public String getProtectionSchedule() { return protectionSchedule; }
    public void setProtectionSchedule(String protectionSchedule) { this.protectionSchedule = protectionSchedule; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

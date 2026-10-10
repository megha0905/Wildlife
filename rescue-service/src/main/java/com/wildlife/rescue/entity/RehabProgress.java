package com.wildlife.rescue.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rehab_progress")
public class RehabProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rescue_id", nullable = false)
    private RescueCase rescue;

    @Column(name = "assessed_on", nullable = false)
    private LocalDate assessedOn;

    @Column(length = 100)
    private String enclosure;

    @Column(name = "behaviour_notes", nullable = false, length = 1000)
    private String behaviourNotes;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "fit_for_release", nullable = false)
    private boolean fitForRelease;

    @Column(name = "assessed_by", nullable = false, length = 120)
    private String assessedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RescueCase getRescue() { return rescue; }
    public void setRescue(RescueCase rescue) { this.rescue = rescue; }
    public LocalDate getAssessedOn() { return assessedOn; }
    public void setAssessedOn(LocalDate assessedOn) { this.assessedOn = assessedOn; }
    public String getEnclosure() { return enclosure; }
    public void setEnclosure(String enclosure) { this.enclosure = enclosure; }
    public String getBehaviourNotes() { return behaviourNotes; }
    public void setBehaviourNotes(String behaviourNotes) { this.behaviourNotes = behaviourNotes; }
    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
    public boolean isFitForRelease() { return fitForRelease; }
    public void setFitForRelease(boolean fitForRelease) { this.fitForRelease = fitForRelease; }
    public String getAssessedBy() { return assessedBy; }
    public void setAssessedBy(String assessedBy) { this.assessedBy = assessedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

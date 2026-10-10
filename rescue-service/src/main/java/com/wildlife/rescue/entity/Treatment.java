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
@Table(name = "treatments")
public class Treatment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rescue_id", nullable = false)
    private RescueCase rescue;

    @Column(name = "treatment_date", nullable = false)
    private LocalDate treatmentDate;

    @Column(nullable = false, length = 500)
    private String diagnosis;

    @Column(nullable = false, length = 1000)
    private String procedure;

    @Column(length = 500)
    private String medications;

    /** resource-service staff id of the vet, if known. */
    @Column(name = "vet_staff_id")
    private Long vetStaffId;

    @Column(name = "vet_name", nullable = false, length = 120)
    private String vetName;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RescueCase getRescue() { return rescue; }
    public void setRescue(RescueCase rescue) { this.rescue = rescue; }
    public LocalDate getTreatmentDate() { return treatmentDate; }
    public void setTreatmentDate(LocalDate treatmentDate) { this.treatmentDate = treatmentDate; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getProcedure() { return procedure; }
    public void setProcedure(String procedure) { this.procedure = procedure; }
    public String getMedications() { return medications; }
    public void setMedications(String medications) { this.medications = medications; }
    public Long getVetStaffId() { return vetStaffId; }
    public void setVetStaffId(Long vetStaffId) { this.vetStaffId = vetStaffId; }
    public String getVetName() { return vetName; }
    public void setVetName(String vetName) { this.vetName = vetName; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

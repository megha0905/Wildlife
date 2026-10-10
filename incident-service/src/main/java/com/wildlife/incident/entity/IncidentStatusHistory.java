package com.wildlife.incident.entity;

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

/** One row per workflow move (CONTRACTS.md 6.1). fromStatus is null for the creation row. */
@Entity
@Table(name = "incident_status_history")
public class IncidentStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private IncidentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private IncidentStatus toStatus;

    @Column(name = "changed_by_user_id")
    private Long changedByUserId;

    @Column(name = "changed_by_name", length = 120)
    private String changedByName;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(length = 2000)
    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }
    public IncidentStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(IncidentStatus fromStatus) { this.fromStatus = fromStatus; }
    public IncidentStatus getToStatus() { return toStatus; }
    public void setToStatus(IncidentStatus toStatus) { this.toStatus = toStatus; }
    public Long getChangedByUserId() { return changedByUserId; }
    public void setChangedByUserId(Long changedByUserId) { this.changedByUserId = changedByUserId; }
    public String getChangedByName() { return changedByName; }
    public void setChangedByName(String changedByName) { this.changedByName = changedByName; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}

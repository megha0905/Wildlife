package com.wildlife.patrol.entity;

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
@Table(name = "patrol_logs")
public class PatrolLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private PatrolSchedule schedule;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "observation_type", nullable = false, length = 30)
    private ObservationType observationType;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "animal_tag_id", length = 30)
    private String animalTagId;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity_hint", length = 10)
    private Severity severityHint;

    @Column(name = "reported_by_user_id")
    private Long reportedByUserId;

    @Column(name = "reported_by_name", length = 120)
    private String reportedByName;

    /** incident-service id, set when the log is escalated. */
    @Column(name = "incident_id")
    private Long incidentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PatrolSchedule getSchedule() { return schedule; }
    public void setSchedule(PatrolSchedule schedule) { this.schedule = schedule; }
    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
    public ObservationType getObservationType() { return observationType; }
    public void setObservationType(ObservationType observationType) { this.observationType = observationType; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAnimalTagId() { return animalTagId; }
    public void setAnimalTagId(String animalTagId) { this.animalTagId = animalTagId; }
    public Severity getSeverityHint() { return severityHint; }
    public void setSeverityHint(Severity severityHint) { this.severityHint = severityHint; }
    public Long getReportedByUserId() { return reportedByUserId; }
    public void setReportedByUserId(Long reportedByUserId) { this.reportedByUserId = reportedByUserId; }
    public String getReportedByName() { return reportedByName; }
    public void setReportedByName(String reportedByName) { this.reportedByName = reportedByName; }
    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

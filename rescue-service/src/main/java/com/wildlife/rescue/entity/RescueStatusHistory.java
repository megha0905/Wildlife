package com.wildlife.rescue.entity;

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
@Table(name = "rescue_status_history")
public class RescueStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rescue_id", nullable = false)
    private RescueCase rescue;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private RescueStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private RescueStatus toStatus;

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
    public RescueCase getRescue() { return rescue; }
    public void setRescue(RescueCase rescue) { this.rescue = rescue; }
    public RescueStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(RescueStatus fromStatus) { this.fromStatus = fromStatus; }
    public RescueStatus getToStatus() { return toStatus; }
    public void setToStatus(RescueStatus toStatus) { this.toStatus = toStatus; }
    public Long getChangedByUserId() { return changedByUserId; }
    public void setChangedByUserId(Long changedByUserId) { this.changedByUserId = changedByUserId; }
    public String getChangedByName() { return changedByName; }
    public void setChangedByName(String changedByName) { this.changedByName = changedByName; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}

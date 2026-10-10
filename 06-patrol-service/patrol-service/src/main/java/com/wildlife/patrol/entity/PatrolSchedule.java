package com.wildlife.patrol.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patrol_schedules")
public class PatrolSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private PatrolTeam team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private PatrolRoute route;

    @Column(name = "patrol_date", nullable = false)
    private LocalDate patrolDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Shift shift;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PatrolStatus status;

    /** resource-service vehicle id (optional). */
    @Column(name = "vehicle_id")
    private Long vehicleId;

    /** resource-service allocation ids created for this schedule. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "patrol_schedule_allocations", joinColumns = @JoinColumn(name = "schedule_id"))
    @Column(name = "allocation_id")
    private List<Long> allocationIds = new ArrayList<>();

    @Column(length = 500)
    private String objective;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_by_name", length = 120)
    private String createdByName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PatrolTeam getTeam() { return team; }
    public void setTeam(PatrolTeam team) { this.team = team; }
    public PatrolRoute getRoute() { return route; }
    public void setRoute(PatrolRoute route) { this.route = route; }
    public LocalDate getPatrolDate() { return patrolDate; }
    public void setPatrolDate(LocalDate patrolDate) { this.patrolDate = patrolDate; }
    public Shift getShift() { return shift; }
    public void setShift(Shift shift) { this.shift = shift; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public PatrolStatus getStatus() { return status; }
    public void setStatus(PatrolStatus status) { this.status = status; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public List<Long> getAllocationIds() { return allocationIds; }
    public void setAllocationIds(List<Long> allocationIds) { this.allocationIds = allocationIds; }
    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    /** Start of the patrol window. */
    public LocalDateTime windowStart() {
        return patrolDate.atTime(startTime);
    }

    /** End of the patrol window; night patrols that end after midnight finish the next day. */
    public LocalDateTime windowEnd() {
        LocalDateTime end = patrolDate.atTime(endTime);
        return endTime.isAfter(startTime) ? end : end.plusDays(1);
    }
}

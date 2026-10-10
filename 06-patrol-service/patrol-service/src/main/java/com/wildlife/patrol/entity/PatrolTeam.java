package com.wildlife.patrol.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patrol_teams")
public class PatrolTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /** habitat-service area id (no FK across services). */
    @Column(name = "area_id", nullable = false)
    private Long areaId;

    @Column(name = "leader_staff_id", nullable = false)
    private Long leaderStaffId;

    @Column(name = "leader_name", length = 120)
    private String leaderName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "patrol_team_members", joinColumns = @JoinColumn(name = "team_id"))
    private List<TeamMember> members = new ArrayList<>();

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getAreaId() { return areaId; }
    public void setAreaId(Long areaId) { this.areaId = areaId; }
    public Long getLeaderStaffId() { return leaderStaffId; }
    public void setLeaderStaffId(Long leaderStaffId) { this.leaderStaffId = leaderStaffId; }
    public String getLeaderName() { return leaderName; }
    public void setLeaderName(String leaderName) { this.leaderName = leaderName; }
    public List<TeamMember> getMembers() { return members; }
    public void setMembers(List<TeamMember> members) { this.members = members; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

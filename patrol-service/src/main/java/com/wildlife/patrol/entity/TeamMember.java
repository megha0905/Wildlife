package com.wildlife.patrol.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** A team member: staff id from resource-service plus name/designation snapshots. */
@Embeddable
public class TeamMember {

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "staff_name", length = 120)
    private String staffName;

    @Column(length = 20)
    private String designation;

    public TeamMember() {
    }

    public TeamMember(Long staffId, String staffName, String designation) {
        this.staffId = staffId;
        this.staffName = staffName;
        this.designation = designation;
    }

    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
}

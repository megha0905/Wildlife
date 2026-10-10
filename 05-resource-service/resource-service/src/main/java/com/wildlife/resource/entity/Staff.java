package com.wildlife.resource.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "staff")
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code", nullable = false, unique = true, length = 20)
    private String employeeCode;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Designation designation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffStatus status;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    /** habitat-service area id (no FK across services). */
    @Column(name = "home_area_id")
    private Long homeAreaId;

    @Column(name = "joined_on")
    private LocalDate joinedOn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Designation getDesignation() { return designation; }
    public void setDesignation(Designation designation) { this.designation = designation; }
    public StaffStatus getStatus() { return status; }
    public void setStatus(StaffStatus status) { this.status = status; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Long getHomeAreaId() { return homeAreaId; }
    public void setHomeAreaId(Long homeAreaId) { this.homeAreaId = homeAreaId; }
    public LocalDate getJoinedOn() { return joinedOn; }
    public void setJoinedOn(LocalDate joinedOn) { this.joinedOn = joinedOn; }
}

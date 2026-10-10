package com.wildlife.programme.entity;

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
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "programmes")
public class Programme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "programme_type", nullable = false, length = 30)
    private ProgrammeType programmeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProgrammeStatus status;

    @Column(nullable = false, length = 1000)
    private String objective;

    @Column(length = 2000)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "budget_inr", precision = 14, scale = 2)
    private BigDecimal budgetInr;

    /** resource-service staff id. */
    @Column(name = "lead_staff_id")
    private Long leadStaffId;

    /** habitat-service area ids. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "programme_areas", joinColumns = @JoinColumn(name = "programme_id"))
    @Column(name = "area_id")
    private Set<Long> areaIds = new LinkedHashSet<>();

    /** wildlife-service species ids. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "programme_species", joinColumns = @JoinColumn(name = "programme_id"))
    @Column(name = "species_id")
    private Set<Long> speciesIds = new LinkedHashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ProgrammeType getProgrammeType() { return programmeType; }
    public void setProgrammeType(ProgrammeType programmeType) { this.programmeType = programmeType; }
    public ProgrammeStatus getStatus() { return status; }
    public void setStatus(ProgrammeStatus status) { this.status = status; }
    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public BigDecimal getBudgetInr() { return budgetInr; }
    public void setBudgetInr(BigDecimal budgetInr) { this.budgetInr = budgetInr; }
    public Long getLeadStaffId() { return leadStaffId; }
    public void setLeadStaffId(Long leadStaffId) { this.leadStaffId = leadStaffId; }
    public Set<Long> getAreaIds() { return areaIds; }
    public void setAreaIds(Set<Long> areaIds) { this.areaIds = areaIds; }
    public Set<Long> getSpeciesIds() { return speciesIds; }
    public void setSpeciesIds(Set<Long> speciesIds) { this.speciesIds = speciesIds; }
}

package com.wildlife.patrol.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Waypoint {

    @Column(nullable = false)
    private Integer sequence;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 100)
    private String label;

    public Waypoint() {
    }

    public Waypoint(Integer sequence, Double latitude, Double longitude, String label) {
        this.sequence = sequence;
        this.latitude = latitude;
        this.longitude = longitude;
        this.label = label;
    }

    public Integer getSequence() { return sequence; }
    public void setSequence(Integer sequence) { this.sequence = sequence; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}

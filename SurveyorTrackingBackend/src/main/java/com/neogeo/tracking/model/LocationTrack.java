package com.neogeo.tracking.model;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import java.time.Instant;

@Entity
@Table(name = "location_track", schema = "public") 
public class LocationTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "surveyor_id", nullable = false)
    private String surveyorId;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant timestamp;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point geom;

    // Constructors
    public LocationTrack() {}

    public LocationTrack(String surveyorId, double latitude, double longitude, Instant timestamp, Point geom) {
        this.surveyorId = surveyorId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.geom = geom;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public String getSurveyorId() {
        return surveyorId;
    }

    public void setSurveyorId(String surveyorId) {
        this.surveyorId = surveyorId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Point getGeom() {
        return geom;
    }

    public void setGeom(Point geom) {
        this.geom = geom;
    }
}



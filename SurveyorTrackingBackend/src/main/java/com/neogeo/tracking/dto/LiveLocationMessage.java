package com.neogeo.tracking.dto;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonSetter;

public class LiveLocationMessage {
    private String surveyorId;
    private double latitude;
    private double longitude;
    private Instant timestamp;

    public LiveLocationMessage() {
    }

    public LiveLocationMessage(String surveyorId, double latitude, double longitude, Instant timestamp) {
        this.surveyorId = surveyorId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    // Getters and Setters
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
    
    /**
     * Sets timestamp from ISO string format (for mobile app compatibility)
     * Mobile app sends timestamps as ISO 8601 strings like "2025-01-26T10:30:00"
     */
    @JsonSetter("timestamp")
    public void setTimestampFromString(String timestampString) {
        if (timestampString == null || timestampString.trim().isEmpty()) {
            this.timestamp = Instant.now();
            return;
        }
        
        try {
            // Try parsing as ISO 8601 instant format first
            this.timestamp = Instant.parse(timestampString);
        } catch (DateTimeParseException e1) {
            try {
                // Try adding 'Z' for UTC if missing
                this.timestamp = Instant.parse(timestampString + "Z");
            } catch (DateTimeParseException e2) {
                // If all parsing fails, use current time
                this.timestamp = Instant.now();
            }
        }
    }

    @Override
    public String toString() {
        return "LiveLocationMessage{" +
                "surveyorId='" + surveyorId + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LiveLocationMessage that = (LiveLocationMessage) o;
        return Double.compare(that.latitude, latitude) == 0 &&
                Double.compare(that.longitude, longitude) == 0 &&
                Objects.equals(surveyorId, that.surveyorId) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(surveyorId, latitude, longitude, timestamp);
    }
}

package com.neogeo.tracking.model;

import jakarta.persistence.*;
import java.util.Objects;
import java.time.Instant;

@Entity
@Table(name = "surveyor")
public class Surveyor {

    @Id
    @Column(name = "id", length = 255)
    private String id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 255)
    private String city;

    @Column(name = "project_name", length = 255)
    private String projectName;
    
    @Column(unique = true, length = 255)
    private String username;
    
    @Column(length = 255)
    private String password;

    @Column(name = "last_activity_timestamp")
    private Instant lastActivityTimestamp;
    
    @Transient
    private boolean online = false;

    // Constructors
    public Surveyor() {}

    public Surveyor(String id, String name, String city, String projectName, 
                   String username, String password) {
        this.id = validateId(id);
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.city = city;
        this.projectName = projectName;
        this.username = username;
        this.password = password;
    }

    // Getters and setters with validation
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = validateId(id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "Name cannot be null");
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // Business methods
    public boolean isAdmin() {
        return id != null && (id.toLowerCase().contains("admin") || 
               (username != null && username.toLowerCase().contains("admin")));
    }

    public boolean isValidSurveyor() {
        return id != null && !id.isBlank() && !isAdmin();
    }

    // Helper methods
    private String validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID cannot be null or blank");
        }
        return id;
    }

    // Online status methods
    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public Instant getLastActivityTimestamp() {
        return lastActivityTimestamp;
    }

    public void setLastActivityTimestamp(Instant lastActivityTimestamp) {
        this.lastActivityTimestamp = lastActivityTimestamp;
    }

    // Equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Surveyor surveyor = (Surveyor) o;
        return Objects.equals(id, surveyor.id) && 
               Objects.equals(username, surveyor.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }

    @Override
    public String toString() {
        return "Surveyor{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", city='" + city + '\'' +
                ", projectName='" + projectName + '\'' +
                ", username='" + username + '\'' +
                ", online=" + online +
                '}';
    }
}

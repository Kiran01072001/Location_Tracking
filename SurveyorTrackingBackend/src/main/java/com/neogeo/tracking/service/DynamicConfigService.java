package com.neogeo.tracking.service;

import com.neogeo.tracking.repository.SurveyorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DynamicConfigService {

    @Autowired
    private SurveyorRepository surveyorRepository;

    // Get all unique cities from database
    public List<String> getAllCities() {
        List<String> cities = surveyorRepository.findAll().stream()
                .map(surveyor -> surveyor.getCity())
                .filter(city -> city != null && !city.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        
        // Add default cities if none found
        if (cities.isEmpty()) {
            cities = Arrays.asList("Hyderabad", "Mumbai", "Delhi", "Bangalore", "Chennai");
        }
        
        return cities;
    }

    // Get all unique projects from database
    public List<String> getAllProjects() {
        List<String> projects = surveyorRepository.findAll().stream()
                .map(surveyor -> surveyor.getProjectName())
                .filter(project -> project != null && !project.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        
        // Add default projects if none found
        if (projects.isEmpty()) {
            projects = Arrays.asList("PTMS", "Survey", "Mapping", "Inspection", "Construction");
        }
        
        return projects;
    }

    // Get all available statuses
    public List<String> getAllStatuses() {
        return Arrays.asList(
            "Online", "Offline", "Busy", "Available", "On Break",
            "In Meeting", "Traveling", "On Site", "Office", "Field Work"
        );
    }

    // Get all available roles
    public List<String> getAllRoles() {
        return Arrays.asList(
            "Surveyor", "Supervisor", "Manager", "Coordinator", "Technician",
            "Engineer", "Analyst", "Consultant", "Inspector", "Planner"
        );
    }

    // Get all dropdown options
    public Map<String, List<String>> getAllDropdownOptions() {
        Map<String, List<String>> dropdowns = new HashMap<>();
        dropdowns.put("cities", getAllCities());
        dropdowns.put("projects", getAllProjects());
        dropdowns.put("statuses", getAllStatuses());
        dropdowns.put("roles", getAllRoles());
        return dropdowns;
    }

    // Add new city
    public void addCity(String city) {
        // This would typically save to a separate configuration table
        // For now, we'll just log it
        System.out.println("New city added: " + city);
    }

    // Add new project
    public void addProject(String project) {
        // This would typically save to a separate configuration table
        // For now, we'll just log it
        System.out.println("New project added: " + project);
    }

    // Get system configuration
    public Map<String, Object> getSystemConfig() {
        Map<String, Object> config = new HashMap<>();
        
        // Map configuration
        Map<String, Object> mapConfig = new HashMap<>();
        mapConfig.put("defaultCenter", Arrays.asList(17.3850, 78.4867)); // Hyderabad
        mapConfig.put("defaultZoom", 10);
        mapConfig.put("trackingInterval", 5000);
        mapConfig.put("refreshInterval", 30000);
        config.put("map", mapConfig);
        
        // Feature flags
        Map<String, Boolean> features = new HashMap<>();
        features.put("realTimeTracking", true);
        features.put("historicalRoutes", true);
        features.put("geofencing", false);
        features.put("notifications", false);
        features.put("offlineMode", false);
        features.put("analytics", false);
        features.put("reporting", false);
        features.put("darkMode", true);
        features.put("customThemes", true);
        features.put("exportData", true);
        config.put("features", features);
        
        // UI configuration
        Map<String, Object> uiConfig = new HashMap<>();
        uiConfig.put("animations", true);
        uiConfig.put("transitions", true);
        uiConfig.put("loadingSpinners", true);
        uiConfig.put("responsive", true);
        config.put("ui", uiConfig);
        
        return config;
    }

    // Update system configuration
    public void updateSystemConfig(Map<String, Object> config) {
        // This would typically save to a configuration table
        System.out.println("System configuration updated: " + config);
    }
} 
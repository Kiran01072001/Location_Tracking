


package com.neogeo.tracking.controller;

import com.neogeo.tracking.service.DynamicConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
public class DynamicConfigController {

    @Autowired
    private DynamicConfigService dynamicConfigService;

    // Get all cities from database
    @GetMapping("/cities")
    public ResponseEntity<List<String>> getAllCities() {
        List<String> cities = dynamicConfigService.getAllCities();
        return ResponseEntity.ok(cities);
    }

    // Get all projects from database
    @GetMapping("/projects")
    public ResponseEntity<List<String>> getAllProjects() {
        List<String> projects = dynamicConfigService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    // Get all statuses
    @GetMapping("/statuses")
    public ResponseEntity<List<String>> getAllStatuses() {
        List<String> statuses = dynamicConfigService.getAllStatuses();
        return ResponseEntity.ok(statuses);
    }

    // Get all roles
    @GetMapping("/roles")
    public ResponseEntity<List<String>> getAllRoles() {
        List<String> roles = dynamicConfigService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    // Get all dropdown options
    @GetMapping("/dropdowns")
    public ResponseEntity<Map<String, List<String>>> getAllDropdownOptions() {
        Map<String, List<String>> dropdowns = dynamicConfigService.getAllDropdownOptions();
        return ResponseEntity.ok(dropdowns);
    }

    // Add new city
    @PostMapping("/cities")
    public ResponseEntity<String> addCity(@RequestBody String city) {
        dynamicConfigService.addCity(city);
        return ResponseEntity.ok("City added successfully");
    }

    // Add new project
    @PostMapping("/projects")
    public ResponseEntity<String> addProject(@RequestBody String project) {
        dynamicConfigService.addProject(project);
        return ResponseEntity.ok("Project added successfully");
    }

    // Get system configuration
    @GetMapping("/system")
    public ResponseEntity<Map<String, Object>> getSystemConfig() {
        Map<String, Object> config = dynamicConfigService.getSystemConfig();
        return ResponseEntity.ok(config);
    }

    // Update system configuration
    @PutMapping("/system")
    public ResponseEntity<String> updateSystemConfig(@RequestBody Map<String, Object> config) {
        dynamicConfigService.updateSystemConfig(config);
        return ResponseEntity.ok("System configuration updated successfully");
    }
} 



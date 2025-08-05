package com.neogeo.tracking.service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.neogeo.tracking.model.Surveyor;
import com.neogeo.tracking.repository.SurveyorRepository;
import com.neogeo.tracking.repository.LocationTrackRepository;

@Service
public class SurveyorService {
    private final SurveyorRepository repository;
    private final LocationTrackRepository locationTrackRepository;
    private final Map<String, Instant> lastActivityMap = new ConcurrentHashMap<>();
    // Consider a surveyor online if they've been active in the last 12 minutes
    // This matches the requirement: if last timestamp <= 12 minutes ago → Online
    private static final long ONLINE_TIMEOUT_SECONDS = 720; // 12 minutes (720 seconds)

    public SurveyorService(SurveyorRepository repository, LocationTrackRepository locationTrackRepository) {
        this.repository = repository;
        this.locationTrackRepository = locationTrackRepository;
    }

    public List<Surveyor> listAll() {
        return repository.findAll();
    }

    /**
     * Gets all surveyors with their online status
     * @return List of surveyors with updated online status
     */
    public List<Surveyor> getAllSurveyorsWithStatus() {
        List<Surveyor> surveyors = listAll();
        for (Surveyor surveyor : surveyors) {
            boolean isOnline = isSurveyorOnline(surveyor.getId());
            surveyor.setOnline(isOnline);
        }
        return surveyors;
    }

    public Surveyor save(Surveyor surveyor) {
        return repository.save(surveyor);
    }

    /**
     * Save or update a surveyor
     * If the surveyor has an ID that exists, it will be updated, otherwise a new surveyor will be created
     * @param surveyor The surveyor to save or update
     * @return The saved or updated surveyor
     */
    public Surveyor saveOrUpdateSurveyor(Surveyor surveyor) {
        // You can add additional logic here such as password hashing
        // before saving the surveyor
        return repository.save(surveyor);
    }

    public List<Surveyor> filter(String city, String project) {
        return repository.findByCityContainingIgnoreCaseAndProjectNameContainingIgnoreCase(
                city == null ? "" : city,
                project == null ? "" : project
        );
    }
    
    public Surveyor findByUsername(String username) {
        return repository.findByUsername(username).orElse(null);
    }
    
    public boolean authenticateSurveyor(String username, String password) {
        Surveyor surveyor = repository.findByUsername(username).orElse(null);
        if (surveyor != null) {
            return password.equals(surveyor.getPassword());
        }
        return false;
    }

    /**
     * Authenticates a surveyor and returns a response with status and details
     * Updates activity status (used for mobile app login)
     * @param username The username to authenticate
     * @param password The password to verify
     * @return A map containing authentication status, HTTP status code, and surveyor details if successful
     */
    public Map<String, Object> authenticateAndGetResponse(String username, String password) {
        Map<String, Object> response = new HashMap<>();
        Surveyor surveyor = repository.findByUsername(username).orElse(null);

        if (surveyor == null) {
            response.put("status", 404);
            response.put("message", "Surveyor not found");
            return response;
        }

        boolean authenticated = password.equals(surveyor.getPassword());

        if (authenticated) {
            response.put("status", 200);
            response.put("authenticated", true);
            response.put("surveyor", surveyor);
            // Update the activity status when authenticated (mobile app login)
            updateSurveyorActivity(surveyor.getId());
            // Persist last activity timestamp in DB
            surveyor.setLastActivityTimestamp(java.time.Instant.now());
            repository.save(surveyor);
        } else {
            response.put("status", 401);
            response.put("authenticated", false);
            response.put("message", "Invalid credentials");
        }

        return response;
    }


    
    public boolean isUsernameAvailable(String username) {
        return !repository.existsByUsername(username);
    }
    
    /**
     * Updates the last activity timestamp for a surveyor
     * @param surveyorId The ID of the surveyor
     */
    public void updateSurveyorActivity(String surveyorId) {
        lastActivityMap.put(surveyorId, Instant.now());
        // Also persist last activity timestamp in DB
        Surveyor surveyor = repository.findById(surveyorId).orElse(null);
        if (surveyor != null) {
            surveyor.setLastActivityTimestamp(java.time.Instant.now());
            repository.save(surveyor);
        }
    }
    
    /**
     * Checks if a surveyor is considered online based on their last GPS timestamp
     * @param surveyorId The ID of the surveyor
     * @return true if the surveyor has been active recently, false otherwise
     */
    public boolean isSurveyorOnline(String surveyorId) {
        // Check the latest GPS timestamp from location_track table
        // This is the most accurate way to determine if a surveyor is online
        java.util.Optional<com.neogeo.tracking.model.LocationTrack> latestLocation = 
            locationTrackRepository.findTopBySurveyorIdOrderByTimestampDesc(surveyorId);
        
        if (latestLocation.isPresent()) {
            Instant lastGpsTimestamp = latestLocation.get().getTimestamp();
            long secondsSinceLastGps = Instant.now().getEpochSecond() - lastGpsTimestamp.getEpochSecond();
            return secondsSinceLastGps <= ONLINE_TIMEOUT_SECONDS;
        }
        
        // Fallback to in-memory or DB timestamp if no GPS data
        Instant lastActivity = lastActivityMap.get(surveyorId);
        if (lastActivity == null) {
            // Fallback to DB timestamp if in-memory is missing
            Surveyor surveyor = repository.findById(surveyorId).orElse(null);
            if (surveyor != null && surveyor.getLastActivityTimestamp() != null) {
                lastActivity = surveyor.getLastActivityTimestamp();
            } else {
                return false;
            }
        }
        
        long secondsSinceLastActivity = Instant.now().getEpochSecond() - lastActivity.getEpochSecond();
        return secondsSinceLastActivity <= ONLINE_TIMEOUT_SECONDS;
    }
    
    /**
     * Gets the online status of all surveyors
     * @return A map of surveyor IDs to their online status
     */
    public Map<String, Boolean> getAllSurveyorStatuses() {
        List<Surveyor> surveyors = listAll();
        Map<String, Boolean> statuses = new ConcurrentHashMap<>();
        
        for (Surveyor surveyor : surveyors) {
            boolean isOnline = isSurveyorOnline(surveyor.getId());
            statuses.put(surveyor.getId(), isOnline);
        }
        
        return statuses;
    }

     /**
     * Deletes a surveyor by ID
     * @param id The ID of the surveyor to delete
     * @return true if deleted, false if not found
     */
    public boolean deleteSurveyorById(String id) {
        if (repository.existsById(id)) {
            // Also delete associated location tracks
            locationTrackRepository.deleteBySurveyorId(id);
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Gets all distinct cities from surveyors
     * @return List of distinct city names
     */
    public List<String> getAllDistinctCities() {
        return repository.findAllDistinctCities();
    }

    /**
     * Gets all distinct project names from surveyors
     * @return List of distinct project names
     */
    public List<String> getAllDistinctProjects() {
        return repository.findAllDistinctProjects();
    }
    
    /**
     * Find surveyor by ID
     * @param id The surveyor ID
     * @return Surveyor object or null if not found
     */
    public Surveyor findById(String id) {
        return repository.findById(id).orElse(null);
    }
    
    /**
     * Gets projects available in a specific city
     * @param city The city name
     * @return List of project names in that city
     */
    public List<String> getProjectsByCity(String city) {
        return repository.findByCity(city).stream()
                .map(Surveyor::getProjectName)
                .filter(project -> project != null && !project.isBlank())
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Gets cities available for a specific project
     * @param project The project name
     * @return List of city names for that project
     */
    public List<String> getCitiesByProject(String project) {
        return repository.findByProjectName(project).stream()
                .map(Surveyor::getCity)
                .filter(city -> city != null && !city.isBlank())
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
    }
}

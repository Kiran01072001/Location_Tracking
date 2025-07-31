
package com.neogeo.tracking;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.neogeo.tracking.model.LocationTrack;
import com.neogeo.tracking.model.Surveyor;
import com.neogeo.tracking.repository.LocationTrackRepository;
import com.neogeo.tracking.repository.SurveyorRepository;
import com.neogeo.tracking.service.SurveyorService;

@Service
public class LocationTrackService {

    private static final int OFFLINE_THRESHOLD_MINUTES = 15; // Match with SurveyorService timeout

    private final LocationTrackRepository locationTrackRepository;
    private final SurveyorRepository surveyorRepository;
    private final SurveyorService surveyorService;

    @Autowired
    public LocationTrackService(LocationTrackRepository locationTrackRepository,
                              SurveyorRepository surveyorRepository,
                              SurveyorService surveyorService) {
        this.locationTrackRepository = locationTrackRepository;
        this.surveyorRepository = surveyorRepository;
        this.surveyorService = surveyorService;
    }

    public List<Surveyor> getAllSurveyorsExcludingAdmin() {
        return surveyorRepository.findAll().stream()
            .filter(this::isValidSurveyor)
            .collect(Collectors.toList());
    }

    public Map<String, String> getSurveyorStatusesExcludingAdmin() {
        Instant now = Instant.now();
        Instant threshold = now.minus(OFFLINE_THRESHOLD_MINUTES, ChronoUnit.MINUTES);
        
        return getAllSurveyorsExcludingAdmin().stream()
            .collect(Collectors.toMap(
                Surveyor::getId,
                surveyor -> determineStatus(surveyor.getId(), threshold)
            ));
    }

    public List<Surveyor> filterSurveyorsExcludingAdmin(String city, String project, String status) {
        List<Surveyor> surveyors = findSurveyorsByFilters(city, project);
        return surveyors.stream()
            .filter(this::isValidSurveyor)
            .peek(this::logSurveyorDetails)
            .collect(Collectors.toList());
    }

    /**
     * Gets all surveyors with their latest location data
     * @return List of surveyors with their latest location information
     */
    public List<Map<String, Object>> getAllSurveyorsWithLatestLocations() {
        List<Surveyor> surveyors = getAllSurveyorsExcludingAdmin();
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Surveyor surveyor : surveyors) {
            Map<String, Object> surveyorData = new HashMap<>();
            surveyorData.put("surveyor", surveyor);
            
            // Get latest location for this surveyor
            LocationTrack latestLocation = getLatestLocation(surveyor.getId());
            if (latestLocation != null) {
                Map<String, Object> locationData = new HashMap<>();
                locationData.put("latitude", latestLocation.getLatitude());
                locationData.put("longitude", latestLocation.getLongitude());
                locationData.put("timestamp", latestLocation.getTimestamp());
                surveyorData.put("latestLocation", locationData);
            }
            
            // Get online status
            boolean isOnline = surveyorService.isSurveyorOnline(surveyor.getId());
            surveyorData.put("online", isOnline);
            
            result.add(surveyorData);
        }
        
        return result;
    }

    public LocationTrack getLatestLocation(String surveyorId) {
        try {
            return locationTrackRepository
                .findTopBySurveyorIdOrderByTimestampDesc(surveyorId)
                .orElse(null);
        } catch (Exception e) {
            System.err.println("Error fetching latest location for surveyor " + surveyorId + ": " + e.getMessage());
            return null;
        }
    }

    public List<LocationTrack> getTrackHistory(String surveyorId, Instant start, Instant end) {
        validateTimeRange(start, end);
        
        List<LocationTrack> results = fetchLocationTracks(surveyorId, start, end);
        logResults(results);
        return results;
    }

    public Page<LocationTrack> getTrackHistoryPaged(String surveyorId, Instant start, Instant end, Pageable pageable) {
        validateTimeRange(start, end);
        return locationTrackRepository.findBySurveyorIdAndTimestampBetweenOrderByTimestampAsc(surveyorId, start, end, pageable);
    }

    private boolean isValidSurveyor(Surveyor surveyor) {
        return surveyor.getId() != null &&
               !surveyor.getId().toLowerCase().contains("admin") &&
               (surveyor.getUsername() == null || !surveyor.getUsername().toLowerCase().contains("admin"));
    }

    /**
     * Get enhanced track history with interpolated points for large gaps
     */
    public List<LocationTrack> getEnhancedTrackHistory(String surveyorId, Instant start, Instant end) {
        List<LocationTrack> originalTracks = fetchLocationTracks(surveyorId, start, end);
        return ensureCompleteRoute(originalTracks);
    }

    /**
     * Public method to ensure complete route data by filling in large gaps between location points
     * @param tracks Original list of location tracks
     * @return Enhanced list with interpolated points for large gaps
     */
    public List<LocationTrack> ensureCompleteRoutePublic(List<LocationTrack> tracks) {
        return ensureCompleteRoute(tracks);
    }

    private String determineStatus(String surveyorId, Instant threshold) {
        LocationTrack lastLocation = getLatestLocation(surveyorId);
        boolean isLocationActive = lastLocation != null && 
                                 lastLocation.getTimestamp().isAfter(threshold);
        boolean isActiveFromStatus = surveyorService.isSurveyorOnline(surveyorId);
        
        return (isLocationActive || isActiveFromStatus) ? "Online" : "Offline";
    }

    private List<Surveyor> findSurveyorsByFilters(String city, String project) {
        if (city != null && project != null) {
            return surveyorRepository.findByCityAndProjectName(city, project);
        } else if (city != null) {
            return surveyorRepository.findByCity(city);
        } else if (project != null) {
            return surveyorRepository.findByProjectName(project);
        }
        return surveyorRepository.findAll();
    }

    private void logSurveyorDetails(Surveyor surveyor) {
        System.out.printf("Surveyor: ID=%s, Name=%s, City=%s, Project=%s%n",
            surveyor.getId(), surveyor.getName(), surveyor.getCity(), surveyor.getProjectName());
    }

    private void validateTimeRange(Instant start, Instant end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
    }

    private List<LocationTrack> fetchLocationTracks(String surveyorId, Instant start, Instant end) {
        if (start != null && end != null) {
            return locationTrackRepository.findBySurveyorIdAndTimestampBetweenOrderByTimestampAsc(
                surveyorId, start, end);
        } else if (start != null) {
            return locationTrackRepository.findBySurveyorIdAndTimestampAfterOrderByTimestampAsc(
                surveyorId, start);
        } else if (end != null) {
            return locationTrackRepository.findBySurveyorIdAndTimestampBeforeOrderByTimestampAsc(
                surveyorId, end);
        }
        return locationTrackRepository.findBySurveyorIdOrderByTimestampAsc(surveyorId);
    }

    private void logResults(List<LocationTrack> results) {
        System.out.printf("Query returned %d records%n", results.size());
        results.stream().limit(3).forEach(track ->
            System.out.printf("  %s at %s -> (%.6f, %.6f)%n",
                track.getSurveyorId(), track.getTimestamp(),
                track.getLatitude(), track.getLongitude())
        );
    }
    
    /**
     * Ensures complete route data by filling in large gaps between location points
     * @param tracks Original list of location tracks
     * @return Enhanced list with interpolated points for large gaps
     */
    private List<LocationTrack> ensureCompleteRoute(List<LocationTrack> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            return tracks;
        }
        
        List<LocationTrack> enhancedTracks = new java.util.ArrayList<>();
        enhancedTracks.add(tracks.get(0)); // Add the first point
        
        // Check for gaps between consecutive points
        for (int i = 1; i < tracks.size(); i++) {
            LocationTrack prev = tracks.get(i-1);
            LocationTrack curr = tracks.get(i);
            
            // Calculate time difference in minutes
            long timeDiffMinutes = java.time.Duration.between(prev.getTimestamp(), curr.getTimestamp()).toMinutes();
            
            // If gap is more than 5 minutes, add interpolated points
            if (timeDiffMinutes > 5) {
                // Calculate distance between points
                double distance = calculateDistance(prev.getLatitude(), prev.getLongitude(), 
                                                 curr.getLatitude(), curr.getLongitude());
                
                // If distance is significant, add intermediate points
                if (distance > 0.1) { // More than 100 meters
                    int pointsToAdd = Math.min((int)(timeDiffMinutes / 2), 10); // Add points every 2 minutes, max 10 points
                    
                    for (int j = 1; j <= pointsToAdd; j++) {
                        // Calculate interpolation factor
                        double factor = (double)j / (pointsToAdd + 1);
                        
                        // Interpolate coordinates
                        double interpLat = prev.getLatitude() + factor * (curr.getLatitude() - prev.getLatitude());
                        double interpLon = prev.getLongitude() + factor * (curr.getLongitude() - prev.getLongitude());
                        
                        // Calculate intermediate timestamp
                        Instant interpTime = prev.getTimestamp().plus(
                            java.time.Duration.between(prev.getTimestamp(), curr.getTimestamp()).multipliedBy((long)(factor * 1000)).toMillis(), 
                            java.time.temporal.ChronoUnit.MILLIS);
                        
                        // Create and add interpolated point
                        LocationTrack interpPoint = new LocationTrack(
                            prev.getSurveyorId(),
                            interpLat,
                            interpLon,
                            interpTime,
                            null // No geometry for interpolated points
                        );
                        enhancedTracks.add(interpPoint);
                    }
                }
            }
            
            // Add the current point
            enhancedTracks.add(curr);
        }
        
        return enhancedTracks;
    }
    
    /**
     * Calculates distance between two points in kilometers
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Earth radius in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                  Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                  Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
}

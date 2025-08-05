package com.neogeo.tracking;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neogeo.tracking.dto.LiveLocationMessage;
import com.neogeo.tracking.model.LocationTrack;
import com.neogeo.tracking.model.Surveyor;
import com.neogeo.tracking.repository.LocationTrackRepository;
import com.neogeo.tracking.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Location Tracking", description = "APIs for tracking surveyor locations")
public class LocationTrackController {

    private final SimpMessagingTemplate messagingTemplate;
    private final LocationTrackRepository repository;
    private final SurveyorService surveyorService;
    private final TracingService tracingService;
    private final ObjectMapper objectMapper;
    private final LocationTrackService locationTrackService;

    @Autowired
    public LocationTrackController(SimpMessagingTemplate messagingTemplate,
                                 LocationTrackRepository repository,
                                 SurveyorService surveyorService,
                                 TracingService tracingService,
                                 LocationTrackService locationTrackService) {
        this.messagingTemplate = messagingTemplate;
        this.repository = repository;
        this.surveyorService = surveyorService;
        this.tracingService = tracingService;
        this.locationTrackService = locationTrackService;
        this.objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Operation(summary = "Filter surveyors")
    @GetMapping("/surveyors/filter")
    public List<Surveyor> filterSurveyors(
            @Parameter(description = "City to filter by") @RequestParam(required = false) String city,
            @Parameter(description = "Project to filter by") @RequestParam(required = false) String project,
            @Parameter(description = "Online status to filter by") @RequestParam(required = false) String status) {
        return locationTrackService.filterSurveyorsExcludingAdmin(city, project, status);
    }

    @Operation(summary = "Get all surveyors with latest locations")
    @GetMapping("/surveyors/with-locations")
    public ResponseEntity<List<Map<String, Object>>> getAllSurveyorsWithLatestLocations() {
        List<Map<String, Object>> surveyorsWithLocations = locationTrackService.getAllSurveyorsWithLatestLocations();
        return ResponseEntity.ok(surveyorsWithLocations);
    }

    @Operation(summary = "Get latest location")
    @GetMapping("/location/{surveyorId}/latest")
    public ResponseEntity<LocationTrack> getLatestLocation(
            @Parameter(description = "ID of the surveyor") @PathVariable String surveyorId) {
        try {
            LocationTrack location = locationTrackService.getLatestLocation(surveyorId);
            if (location != null) {
                // Debug logging
                System.out.printf("📍 Latest location for surveyor %s: (%.6f, %.6f) at %s%n", 
                    surveyorId, location.getLatitude(), location.getLongitude(), location.getTimestamp());
                    
                return ResponseEntity.ok()
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("Expires", "0")
                    .body(location);
            } else {
                System.out.printf("📍 No location found for surveyor %s%n", surveyorId);
                return ResponseEntity.noContent()
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("Expires", "0")
                    .build();
            }
        } catch (Exception e) {
            System.err.printf("❌ Error getting latest location for surveyor %s: %s%n", surveyorId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Get location history")
    @GetMapping("/location/{surveyorId}/track")
    public ResponseEntity<Page<LocationTrack>> getTrackHistory(
            @PathVariable String surveyorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size) {

        if (start.isAfter(end)) {
            return ResponseEntity.badRequest().build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<LocationTrack> tracks = locationTrackService.getTrackHistoryPaged(surveyorId, start, end, pageable);
        
        // Ensure we capture the final GPS point before logout/stop tracking
        // This is handled by the mobile app sending a final location update
        return tracks.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(tracks);
    }

    @Operation(summary = "Get enhanced location history with interpolated points")
    @GetMapping("/location/{surveyorId}/enhanced-track")
    public ResponseEntity<List<LocationTrack>> getEnhancedTrackHistory(
            @PathVariable String surveyorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {

        if (start.isAfter(end)) {
            return ResponseEntity.badRequest().build();
        }

        List<LocationTrack> tracks = locationTrackService.getEnhancedTrackHistory(surveyorId, start, end);
        return tracks.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(tracks);
    }
    
    @Operation(summary = "Get projects by city for cascading filters")
    @GetMapping("/filters/cities/{city}/projects")
    public ResponseEntity<List<String>> getProjectsByCity(@PathVariable String city) {
        List<String> projects = locationTrackService.getProjectsByCity(city);
        return ResponseEntity.ok(projects);
    }
    
    @Operation(summary = "Get cities by project for cascading filters")
    @GetMapping("/filters/projects/{project}/cities")
    public ResponseEntity<List<String>> getCitiesByProject(@PathVariable String project) {
        List<String> cities = locationTrackService.getCitiesByProject(project);
        return ResponseEntity.ok(cities);
    }

    @GetMapping("/surveyors/status")
    public Map<String, String> getSurveyorStatus() {
        return locationTrackService.getSurveyorStatusesExcludingAdmin();
    }

    @Operation(summary = "Get total distance travelled by surveyor")
    @GetMapping("/location/{surveyorId}/distance")
    public ResponseEntity<Map<String, Double>> getTotalDistance(@PathVariable String surveyorId) {
        try {
            double totalDistance = locationTrackService.getTotalDistance(surveyorId);
            return ResponseEntity.ok(Map.of("totalDistance", totalDistance));
        } catch (Exception e) {
            System.err.printf("Error calculating total distance for surveyor %s: %s%n", surveyorId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Update live location")
    @PostMapping("/live/location")
    public ResponseEntity<String> publishLiveLocation(
            @RequestBody LiveLocationMessage message,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return tracingService.traceGpsOperation("location-update", message.getSurveyorId(), 1, () -> {
            if (!validateAuth(authHeader)) {
                System.err.println("Unauthorized access attempt for surveyor: " + message.getSurveyorId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }

            try {
                // Validate timestamp presence and correctness
                if (message.getTimestamp() == null) {
                    System.out.printf("⚠️ Missing timestamp for surveyor %s, using current time%n", message.getSurveyorId());
                } else if (message.getTimestamp().isAfter(java.time.Instant.now().plusSeconds(60))) {
                    System.err.printf("⚠️ Future timestamp detected for surveyor %s: %s%n", message.getSurveyorId(), message.getTimestamp());
                }

                // Always save location first - NEVER drop any GPS points
                saveLocation(message);

                // Then broadcast to live tracking
                broadcastLocation(message);

                // Update surveyor activity timestamp on location update
                surveyorService.updateSurveyorActivity(message.getSurveyorId());

                // Log successful GPS capture for debugging with frequency info
                long totalPoints = locationTrackService.getLocationCount(message.getSurveyorId());
                System.out.printf("📍 GPS CAPTURED: Surveyor %s at (%.6f, %.6f) - Total points: %d%n",
                    message.getSurveyorId(), message.getLatitude(), message.getLongitude(), totalPoints);

                // Log frequency stats every 10 points
                if (totalPoints % 10 == 0) {
                    System.out.printf("📊 GPS STATS: Surveyor %s has reached %d total GPS points%n",
                        message.getSurveyorId(), totalPoints);
                }

                return ResponseEntity.ok("Location updated and stored");
            } catch (JsonProcessingException e) {
                System.err.println("JSON processing error for surveyor " + message.getSurveyorId() + ": " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Processing error");
            } catch (Exception e) {
                System.err.println("Unexpected error processing location for surveyor " + message.getSurveyorId() + ": " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
            }
        });
    }

    // Mobile app compatibility endpoint - maps to the same functionality
    @Operation(summary = "Update location (Mobile App)")
    @PostMapping("/location")
    public ResponseEntity<String> updateLocation(
            @RequestBody LiveLocationMessage message,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Delegate to the main live location endpoint
        return publishLiveLocation(message, authHeader);
    }

    @Operation(summary = "Final location update on logout/stop",
               description = "Captures final GPS position when surveyor logs out or stops tracking")
    @PostMapping("/location/final")
    public ResponseEntity<String> finalLocationUpdate(
            @RequestBody LiveLocationMessage message,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return tracingService.traceGpsOperation("final-location-update", message.getSurveyorId(), 1, () -> {
            if (!validateAuth(authHeader)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }

            try {
                // Force immediate save with final flag in log
                saveLocation(message);

                // Log as final position
                System.out.printf("🏁 FINAL GPS POSITION: Surveyor %s at (%.6f, %.6f) - SESSION END%n",
                    message.getSurveyorId(), message.getLatitude(), message.getLongitude());

                // Update surveyor activity timestamp for final logout
                surveyorService.updateSurveyorActivity(message.getSurveyorId());

                return ResponseEntity.ok("Final location captured successfully");
            } catch (Exception e) {
                System.err.printf("❌ Failed to capture final location for surveyor %s: %s%n",
                    message.getSurveyorId(), e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to capture final location");
            }
        });
    }

    @Operation(summary = "Batch location updates",
               description = "Accepts multiple GPS points at once for better data capture during poor connectivity")
    @PostMapping("/location/batch")
    public ResponseEntity<String> batchLocationUpdate(
            @RequestBody List<LiveLocationMessage> messages,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (messages == null || messages.isEmpty()) {
            return ResponseEntity.badRequest().body("No location data provided");
        }

        return tracingService.traceGpsOperation("batch-location-update",
                messages.get(0).getSurveyorId(), messages.size(), () -> {
            if (!validateAuth(authHeader)) {
                System.err.println("Unauthorized batch location update attempt");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }

            try {
                int successCount = 0;
                int failCount = 0;

                String surveyorId = null;
                StringBuilder errorMessages = new StringBuilder();

                for (LiveLocationMessage message : messages) {
                    try {
                        saveLocation(message);
                        successCount++;
                        if (surveyorId == null) {
                            surveyorId = message.getSurveyorId();
                        }
                    } catch (Exception e) {
                        failCount++;
                        String errorMsg = String.format("❌ Failed to save batch location for surveyor %s: %s%n",
                                message.getSurveyorId(), e.getMessage());
                        System.err.print(errorMsg);
                        errorMessages.append(errorMsg);
                    }
                }

                // Update surveyor activity timestamp after batch
                if (surveyorId != null) {
                    surveyorService.updateSurveyorActivity(surveyorId);
                }

                System.out.printf("📦 BATCH GPS SAVED: %d successful, %d failed for surveyor %s%n",
                        successCount, failCount, surveyorId);

                String responseMsg = String.format("Batch processed: %d successful, %d failed",
                        successCount, failCount);
                if (failCount > 0) {
                    responseMsg += ". Errors: " + errorMessages.toString();
                }

                return ResponseEntity.ok(responseMsg);
            } catch (Exception e) {
                System.err.println("Unexpected error during batch location update: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Batch processing failed");
            }
        });
    }
    private boolean validateAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) return false;

        try {
            String[] credentials = new String(Base64.getDecoder().decode(
                authHeader.substring("Basic ".length()))).split(":", 2);
            return credentials.length == 2 &&
                   surveyorService.authenticateSurveyor(credentials[0], credentials[1]);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void broadcastLocation(LiveLocationMessage message) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(message);
        messagingTemplate.convertAndSend("/topic/location/" + message.getSurveyorId(), json);
    }

    private void saveLocation(LiveLocationMessage message) {
        try {
            // Validate input data
            if (message.getSurveyorId() == null || message.getSurveyorId().trim().isEmpty()) {
                System.err.println("Invalid surveyor ID - cannot save location");
                return;
            }

            // Validate coordinates
            if (message.getLatitude() < -90 || message.getLatitude() > 90 ||
                message.getLongitude() < -180 || message.getLongitude() > 180) {
                System.err.printf("Invalid coordinates for surveyor %s: lat=%.6f, lon=%.6f%n",
                    message.getSurveyorId(), message.getLatitude(), message.getLongitude());
                return;
            }

            Instant timestamp;
            if (message.getTimestamp() != null) {
                timestamp = message.getTimestamp();
            } else {
                timestamp = Instant.now();
                System.out.printf("No timestamp provided for surveyor %s, using current time%n",
                    message.getSurveyorId());
            }

            // Check if this is a duplicate or very close to the previous location
            LocationTrack previousLocation = locationTrackService.getLatestLocation(message.getSurveyorId());
            boolean shouldSave = true;

            if (previousLocation != null) {
                // Calculate time difference in minutes
                long timeDiffMinutes = java.time.Duration.between(previousLocation.getTimestamp(), timestamp).toMinutes();

                // Calculate distance in meters
                double distance = calculateDistance(
                    previousLocation.getLatitude(), previousLocation.getLongitude(),
                    message.getLatitude(), message.getLongitude()
                ) * 1000; // Convert km to meters

                // Only skip if both time and distance are very small
                // This ensures we capture all meaningful movements
                if (timeDiffMinutes < 1 && distance < 10) {
                    // Less than 1 minute and less than 10 meters - consider as duplicate
                    shouldSave = false;
                    System.out.printf("ℹ️ Skipping duplicate location for surveyor %s: time diff=%d min, distance=%.2f m%n",
                        message.getSurveyorId(), timeDiffMinutes, distance);
                }
            }

            if (shouldSave) {
                LocationTrack locationTrack = new LocationTrack(
                    message.getSurveyorId(),
                    message.getLatitude(),
                    message.getLongitude(),
                    timestamp,
                    null
                );

                LocationTrack saved = repository.save(locationTrack);
                System.out.printf("✅ Successfully saved location ID=%d for surveyor %s at %s (%.6f, %.6f)%n",
                    saved.getId(), message.getSurveyorId(), timestamp.toString(),
                    message.getLatitude(), message.getLongitude());

                // Log GPS capture statistics
                long totalPoints = locationTrackService.getLocationCount(message.getSurveyorId());
                if (totalPoints % 5 == 0) {
                    System.out.printf("📊 GPS STATS: Surveyor %s has reached %d total GPS points%n",
                        message.getSurveyorId(), totalPoints);
                }
            }

            // Always update last activity timestamp, even if we skipped saving the location
            surveyorService.updateSurveyorActivity(message.getSurveyorId());

        } catch (Exception e) {
            System.err.printf("❌ Failed to save location for surveyor %s: %s%n",
                message.getSurveyorId(), e.getMessage());
            e.printStackTrace();
            throw e;
        }
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
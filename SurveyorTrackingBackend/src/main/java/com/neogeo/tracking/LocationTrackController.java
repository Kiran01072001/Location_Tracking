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
        LocationTrack location = locationTrackService.getLatestLocation(surveyorId);
        if (location != null) {
            return ResponseEntity.ok(location);
        } else {
            return ResponseEntity.noContent().build();
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

    @GetMapping("/surveyors/status")
    public Map<String, String> getSurveyorStatus() {
        return locationTrackService.getSurveyorStatusesExcludingAdmin();
    }

    @Operation(summary = "Update live location")
    @PostMapping("/live/location")
    public ResponseEntity<String> publishLiveLocation(
            @RequestBody LiveLocationMessage message,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        return tracingService.traceGpsOperation("location-update", message.getSurveyorId(), 1, () -> {
            if (!validateAuth(authHeader)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
            }

            try {
                broadcastLocation(message);
                saveLocation(message);
                // Update surveyor activity timestamp on location update
                surveyorService.updateSurveyorActivity(message.getSurveyorId());
                return ResponseEntity.ok("Location updated");
            } catch (JsonProcessingException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Processing error");
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
                
        } catch (Exception e) {
            System.err.printf("❌ Failed to save location for surveyor %s: %s%n",
                message.getSurveyorId(), e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
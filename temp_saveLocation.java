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
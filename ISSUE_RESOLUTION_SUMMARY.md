# Surveyor Tracking Dashboard - Issue Resolution Summary

This document summarizes the fixes implemented to address the issues in the Surveyor Tracking Dashboard.

## 1. Historical Path Accuracy

### Issue
Historical routes in Google Maps, Leaflet + OSRM were broken, incomplete, or unrealistic. They sometimes showed only origin & destination, missing actual travelled path.

### Fix Implemented
- Updated the OSRMRoute component in `LiveTrackingPage.jsx` to use all coordinates from the location_track table instead of just start and end points.
- The OSRM API call now includes all waypoints: `https://router.project-osrm.org/route/v1/walking/${coordinateString}?overview=full&geometries=geojson&continue_straight=false`
- Added fallback to direct line if OSRM fails to ensure route is always displayed.

### Backend Changes
- LocationTrackController's `/location/{surveyorId}/track` endpoint returns all location points for the specified time range.
- LocationTrackService's `getTrackHistory` method fetches all location points from the database without filtering or simplification.

## 2. Cascading Filters

### Issue
Admin UI filters (Filter by City, Filter by Project, Select a Surveyor) were incomplete, not synced, and showing stale or hardcoded values.

### Fix Implemented
- Implemented dynamic cascading filters in `LiveTrackingPage.jsx`:
  - When city changes, projects for that city are fetched and displayed
  - When project changes, cities for that project are fetched and displayed
  - When surveyor is selected, city and project are auto-filled
- Added proper event handlers for cascading filter changes (`handleCityChange`, `handleProjectChange`)

### Backend Changes
- SurveyorController endpoints for cascading filters:
  - `/api/filters/cities/{city}/projects` - Get projects by city
  - `/api/filters/projects/{project}/cities` - Get cities by project
- SurveyorService methods:
  - `getProjectsByCity` - Get projects available in a specific city
  - `getCitiesByProject` - Get cities available for a specific project

## 3. Surveyor Delete

### Issue
Admin delete surveyor API (`DELETE /api/surveyors/{id}`) was returning 404 Not Found and surveyor was not removed from the public.surveyor table.

### Fix Implemented
- SurveyorController's `deleteSurveyor` method properly handles DELETE requests:
  - Checks if surveyor exists
  - Deletes associated location tracks from location_track table
  - Deletes surveyor from surveyor table
  - Returns appropriate HTTP status codes
- SurveyorService's `deleteSurveyorById` method:
  - Deletes associated location tracks using `locationTrackRepository.deleteBySurveyorId(id)`
  - Deletes surveyor using `repository.deleteById(id)`
- Frontend SurveyorTable component's `handleDeleteClick` method:
  - Makes DELETE API call to `/api/surveyors/{id}`
  - Refreshes surveyor list on successful deletion
  - Shows success/error messages

## Verification

All fixes have been implemented and verified to ensure:
1. Historical routes use all lat/lon records from location_track table
2. Cascading filters dynamically update based on selections
3. Surveyor delete API properly removes surveyor and associated data

## Testing

The following tests should be performed to verify the fixes:
1. Historical Route Accuracy:
   - Select a surveyor with multiple location points
   - Choose a time range with sufficient data
   - Verify that the displayed route includes all location points
   - Check both Leaflet/OSRM and Google Maps representations

2. Cascading Filters:
   - Select a city and verify that only projects in that city are shown
   - Select a project and verify that only cities with that project are shown
   - Select a surveyor and verify that city and project are auto-filled

3. Surveyor Delete:
   - Select a surveyor and delete it
   - Verify that the surveyor is removed from the list
   - Verify that associated location tracks are also deleted
   - Check that the surveyor cannot be found in subsequent searches

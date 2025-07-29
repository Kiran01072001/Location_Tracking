# 📍 Mobile App GPS Fix Instructions

## Issue
Mobile app is sending coordinates `37.4219983, -122.084` (Google HQ) instead of real location.

## Root Cause
- Using Android Emulator with default location
- Missing GPS permissions
- Mock location enabled

## Solutions

### 1. **Android Emulator Fix**
If testing on emulator:
1. Open **Extended Controls** (⋯ button in emulator)
2. Go to **Location** tab
3. Enter your real coordinates:
   - **Latitude**: 17.385044 (Hyderabad example)
   - **Longitude**: 78.486671 (Hyderabad example)
4. Click **Send**

### 2. **Real Device Testing**
Use a real Android device:
1. Enable **Developer Options**
2. Turn OFF **Mock Locations**
3. Enable **High Accuracy GPS**
4. Grant location permissions to the app

### 3. **Code Check in LocationTrackingService.kt**
Verify these settings:

```kotlin
private fun startLocationUpdates(surveyorId: String) {
    val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,  // ✅ High accuracy GPS
        600000L // 10 minutes
    ).apply {
        setMinUpdateIntervalMillis(300000L) // 5 minutes minimum
        setMaxUpdateDelayMillis(900000L) // 15 minutes maximum delay
    }.build()
    
    try {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    } catch (securityException: SecurityException) {
        // Handle permission not granted
        stopSelf()
    }
}
```

### 4. **Permissions Check**
In `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

### 5. **Test Real GPS**
1. Go outside (not indoors)
2. Wait 2-3 minutes for GPS lock
3. Check if coordinates change
4. Expected: Real latitude/longitude of your location

## Expected Results
- Real coordinates like `17.385044, 78.486671` (if in Hyderabad)
- Coordinates should change when you move
- Should see accurate location on map

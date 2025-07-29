# Mobile App Authentication Fix

## Issue
The mobile app is using hardcoded credentials "admin:admin123" for Basic Auth, which don't exist in the database.

## Solution
Update the mobile app to use the logged-in surveyor's credentials for location updates.

## Required Changes

### 1. Update ApiClient.kt

Replace the hardcoded credentials in the `basicAuthInterceptor`:

```kotlin
// OLD CODE (CURRENT):
private val basicAuthInterceptor = Interceptor { chain ->
    val request = chain.request()
    
    // Add Basic Auth header for location update requests
    val newRequest = if (request.url.encodedPath.contains("live/location")) {
        val credentials = "admin:admin123"  // ❌ HARDCODED - DOESN'T EXIST
        val basicAuth = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
        request.newBuilder()
            .addHeader("Authorization", basicAuth)
            .build()
    } else {
        request
    }
    
    chain.proceed(newRequest)
}

// NEW CODE (FIXED):
private val basicAuthInterceptor = Interceptor { chain ->
    val request = chain.request()
    
    // Add Basic Auth header for location update requests
    val newRequest = if (request.url.encodedPath.contains("live/location")) {
        // Get current logged-in surveyor credentials from SharedPreferences or AuthViewModel
        val currentSurveyor = getCurrentLoggedInSurveyor() // You need to implement this
        if (currentSurveyor != null) {
            val credentials = "${currentSurveyor.username}:${currentSurveyor.password}"
            val basicAuth = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            request.newBuilder()
                .addHeader("Authorization", basicAuth)
                .build()
        } else {
            request // No auth if no logged-in user
        }
    } else {
        request
    }
    
    chain.proceed(newRequest)
}

// Helper method to get current logged-in surveyor
private fun getCurrentLoggedInSurveyor(): Surveyor? {
    // Implementation depends on how you store the logged-in user
    // Option 1: From SharedPreferences
    // Option 2: From a singleton/repository
    // Option 3: Pass it as parameter to ApiClient
    return null // Replace with actual implementation
}
```

### 2. Alternative Solution: Pass Credentials Dynamically

Instead of hardcoding in the interceptor, pass credentials when making the API call:

```kotlin
// Update LocationTrackingService.kt
private fun handleLocationUpdate(location: Location) {
    serviceScope.launch {
        try {
            val surveyorId = currentSurveyorId ?: return@launch
            
            // Get current surveyor credentials
            val surveyor = getCurrentSurveyor() // You need to implement this
            if (surveyor == null) {
                Log.e("LocationService", "No surveyor credentials available")
                return@launch
            }
            
            // Create credentials for this specific request
            val credentials = "${surveyor.username}:${surveyor.password}"
            val basicAuth = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            
            val liveLocationMessage = LiveLocationMessage(
                surveyorId = surveyorId,
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = timestamp
            )
            
            // Make API call with proper authentication
            val response = ApiClient.apiService.updateLocationWithAuth(liveLocationMessage, basicAuth)
            
            // ... rest of the code
        } catch (e: Exception) {
            Log.e("LocationService", "Error sending location update", e)
        }
    }
}
```

### 3. Valid Credentials Available

Based on the database query, here are valid surveyor credentials you can test with:

```
Username: vyas, Password: vyas123
Username: Kiran, Password: Kiran@321
Username: Sailaja, Password: Sailaja@987
Username: NeoGeo, Password: NeoGeo@123
Username: test_surveyor, Password: test123
```

## Testing

After implementing the fix, test with a valid surveyor:

```bash
# Test with vyas credentials (Base64: dnlhczp2eWFzMTIz)
curl -X POST http://183.82.114.29:6565/api/live/location \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic dnlhczp2eWFzMTIz" \
  -d '{"surveyorId":"SURV010","latitude":17.385044,"longitude":78.486671,"timestamp":"2025-07-28T10:30:00"}'
```

Expected Response: `Location accepted`

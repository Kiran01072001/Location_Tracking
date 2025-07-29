package com.surveyor.tracking.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.surveyor.tracking.R // Make sure you have an ic_location icon in your res/drawable folder
import com.surveyor.tracking.api.ApiClient
import com.surveyor.tracking.model.LiveLocationMessage
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentSurveyorId: String? = null

    companion object {
        private const val CHANNEL_ID = "LocationTrackingChannel"
        private const val NOTIFICATION_ID = 1
        const val EXTRA_SURVEYOR_ID = "surveyor_id"

        fun startService(context: Context, surveyorId: String) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                putExtra(EXTRA_SURVEYOR_ID, surveyorId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentSurveyorId = intent?.getStringExtra(EXTRA_SURVEYOR_ID)
        if (currentSurveyorId == null) {
            Log.e("LocationService", "Service started without a surveyor ID. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        Log.d("LocationService", "Starting location tracking for surveyor: $currentSurveyorId")

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // --- THE FIX: Call the function without parameters ---
        startLocationUpdates()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        serviceScope.cancel()
        Log.d("LocationService", "Location service destroyed.")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking Service",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Keeps surveyor tracking active in the background."
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val surveyorInfo = "Tracking for ID: ${currentSurveyorId ?: "Unknown"}"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Surveyor Tracking Active")
            .setContentText("Sending location updates every 1 minute. $surveyorInfo")
            .setSmallIcon(R.drawable.ic_location)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .build()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d("LocationService", "New location received: Lat ${location.latitude}, Lon ${location.longitude}")
                    handleLocationUpdate(location)
                }
            }
        }
    }

    // --- THE FIX: The function no longer needs a parameter ---
    private fun startLocationUpdates() {
        // ✅ IMPROVED: More frequent updates for better real-time tracking
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            60000L // 1 minute (60 seconds) for better balance of battery and accuracy
        ).apply {
            setMinUpdateIntervalMillis(30000L) // Minimum update interval of 30 seconds
            setMinUpdateDistanceMeters(10.0f) // Update if moved 10+ meters
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.i("LocationService", "Location updates have been started successfully.")
        } catch (e: SecurityException) {
            Log.e("LocationService", "Location permission not granted. Cannot start updates.", e)
            stopSelf()
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.i("LocationService", "Location updates have been stopped.")
    }

    private fun handleLocationUpdate(location: Location) {
        val surveyorId = currentSurveyorId ?: return

        serviceScope.launch {
            try {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.UK)
                isoFormat.timeZone = TimeZone.getTimeZone("UTC")
                val timestamp = isoFormat.format(Date(location.time))

                val liveLocationMessage = LiveLocationMessage(
                    surveyorId = surveyorId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = timestamp
                )

                Log.d("LocationService", "Sending to backend: $liveLocationMessage")
                val response = ApiClient.apiService.updateLocation(liveLocationMessage)

                if (response.isSuccessful) {
                    Log.i("LocationService", "Location update sent successfully.")
                } else {
                    Log.e("LocationService", "Failed to send update. Code: ${response.code()}, Message: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("LocationService", "Exception while sending location update", e)
            }
        }
    }
}
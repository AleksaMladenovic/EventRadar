package com.eventradar.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.eventradar.R
import com.eventradar.data.model.Event
import com.eventradar.data.repository.AuthRepository
import com.eventradar.data.repository.EventRepository
import com.eventradar.data.repository.LocationRepository
import com.eventradar.data.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {

    @Inject
    lateinit var locationRepository: LocationRepository
    @Inject
    lateinit var userRepository: UserRepository
    @Inject
    lateinit var authRepository: AuthRepository
    @Inject
    lateinit var eventRepository: EventRepository


    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var locationJob: Job? = null
    private var eventsJob: Job? = null
    private var nearbyEventsCache = listOf<Event>()
    private val notifiedEventIds = mutableSetOf<String>()
    private var serviceStartTime: Long = 0L

    override fun onBind(intent: Intent?): IBinder? {
        // Ne koristimo 'binding', pa vraćamo null
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        serviceStartTime = System.currentTimeMillis()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun start() {
        // 1. Kreiraj kanal za notifikacije (obavezno za API 26+)
        val channel = NotificationChannel(
            "location_channel",
            "Location Updates",
            NotificationManager.IMPORTANCE_LOW // Niska važnost da ne smeta korisniku
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        // 2. Kreiraj stalnu notifikaciju
        val notification = NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("EventRadar is Active")
            .setContentText("Tracking your location to find nearby events.")
            .setSmallIcon(R.drawable.app_logo) // Zameni sa ikonicom aplikacije
            .setOngoing(true) // Notifikacija se ne može odbaciti
            .build()

        // 3. Pokreni servis u 'foreground' modu
        startForeground(1, notification) // ID mora biti > 0

        locationJob = locationRepository.getLocationUpdates()
            .onEach { location ->
                // Ažuriraj našu lokaciju na Firestore
                authRepository.getCurrentUserId()?.let { userId ->
                    userRepository.updateUserLocation(userId, location)
                }

                // Proveri lokalni keš događaja u odnosu na novu lokaciju
                checkForNearbyEvents(location)
            }
            .launchIn(serviceScope)

        // Job #2: Prati SVE događaje sa servera
        eventsJob = eventRepository.getFilteredEvents() // Bez filtera za radijus
            .onEach { result ->
                result.onSuccess { events ->
                    // Ažuriraj naš lokalni keš
                    println("LOCATION_SERVICE: Updated events cache with ${events.size} events.")
                    nearbyEventsCache = events
                }
            }
            .launchIn(serviceScope)

    }

    private fun checkForNearbyEvents(myLocation: Location) {
        val serviceStartTimeInSeconds = serviceStartTime / 1000

        nearbyEventsCache.forEach { event ->
            val isEventNew = event.createdAt != null && event.createdAt.seconds > serviceStartTimeInSeconds

            if(isEventNew){
                val results = FloatArray(1)
                Location.distanceBetween(
                    myLocation.latitude, myLocation.longitude,
                    event.location.latitude, event.location.longitude,
                    results
                )
                val distanceInMeters = results[0]

                if (distanceInMeters < 1000 && !notifiedEventIds.contains(event.id)) { // Radijus 1km
                    showNearbyEventNotification(event)
                    notifiedEventIds.add(event.id)
                }
            }

        }
    }

    private fun showNearbyEventNotification(event: Event) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("Nearby Event Alert!")
            .setContentText("A new event '${event.name}' is happening near you.")
            .setSmallIcon(R.drawable.app_logo) // Koristi istu ikonicu
            .setAutoCancel(true) // Notifikacija nestaje kad se klikne
            .build()

        // Koristimo ID događaja (konvertovan u Int) da bi svaka notifikacija bila jedinstvena
        notificationManager.notify(event.id.hashCode(), notification)
    }

    private fun stop() {
        println("LOCATION_SERVICE: Stopping service and location tracking.")
        // Otkaži Job pre zaustavljanja servisa
        locationJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // Poziva se kada se servis uništi
    override fun onDestroy() {
        super.onDestroy()
        // Osiguraj da se sve otkaže
        locationJob?.cancel()
        println("LOCATION_SERVICE: Service destroyed.")
    }

    // Definišemo akcije kao konstante
    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }
}

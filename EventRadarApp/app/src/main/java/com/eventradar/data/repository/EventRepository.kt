package com.eventradar.data.repository

import android.location.Location
import com.eventradar.data.model.Event
import com.eventradar.data.model.EventCategory
import com.eventradar.data.model.EventFilters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import org.imperiumlabs.geofirestore.GeoFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.imperiumlabs.geofirestore.GeoQuery
import org.imperiumlabs.geofirestore.listeners.GeoQueryEventListener
import kotlin.coroutines.resume

@Singleton
class EventRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userRepository: UserRepository,
    private val geoFirestore: GeoFirestore,
    private val filterRepository: FilterRepository,
    private val locationRepository: LocationRepository
) {
    suspend fun addEvent(event: Event): Result<Unit> {
        return try {
            val documentRef = firestore.collection("events").document()

            // --- ISPRAVNA LOGIKA ZA ČEKANJE SA CompletionListener ---
            suspendCancellableCoroutine<Unit> { continuation ->
                geoFirestore.setLocation(
                    documentRef.id,
                    event.location,
                    object : GeoFirestore.CompletionCallback { // <-- ISPRAVAN TIP
                        override fun onComplete(exception: Exception?) {
                            if (exception == null) {
                                // Uspešno, nastavi korutinu
                                continuation.resume(Unit)
                            } else {
                                // Greška, prekini korutinu sa izuzetkom
                                continuation.resumeWithException(exception)
                            }
                        }
                    }
                )
            }

            // Ovaj kod će se izvršiti tek NAKON što je setLocation uspešno završen.
            // Kreiramo mapu sa SVIM OSTALIM podacima
            val eventData = mapOf(
                "id" to documentRef.id,
                "name" to event.name,
                "description" to event.description,
                "category" to event.category,
                "createdAt" to event.createdAt,
                "creatorId" to event.creatorId,
                "creatorName" to event.creatorName,
                "eventTimestamp" to event.eventTimestamp,
                "ageRestriction" to event.ageRestriction,
                "price" to event.price,
                "isFree" to event.isFree,
                "eventImageUrl" to event.eventImageUrl
            )

            // Koristimo 'update' da dodamo ostatak podataka bez pregaženja 'g' i 'l'
            documentRef.update(eventData).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getFilteredEvents(): Flow<Result<List<Event>>> {
        val significantLocationUpdates = locationRepository.getLocationUpdates()
            .distinctUntilChanged { old, new ->
                old.distanceTo(new) < 100f // Emituj samo ako je promena veća od 100m
            }
            .onStart<Location?> { emit(null) } // Odmah emituj null da bi upit krenuo

        return combine(
            filterRepository.filters,
            significantLocationUpdates
        ) { filters, location ->
            Pair(filters, location)
        }.flatMapLatest { (filters, location) ->
            if (filters.radiusInKm != null && location != null) {
                createGeoQueryFlow(filters, location)
            } else {
                createNormalQueryFlow(filters)
            }
        }
    }

    private fun createNormalQueryFlow(filters: EventFilters): Flow<Result<List<Event>>> = callbackFlow {
        var query: Query = firestore.collection("events")

        if (filters.categories.isNotEmpty()) {
            query = query.whereIn("category", filters.categories.map { it.name })
        }
        filters.startDate?.let {
            query = query.whereGreaterThanOrEqualTo("eventTimestamp", it)
        }
        filters.endDate?.let {
            query = query.whereLessThanOrEqualTo("eventTimestamp", it)
        }
        query = query.orderBy("eventTimestamp", Query.Direction.ASCENDING)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error)); return@addSnapshotListener
            }
            snapshot?.let { trySend(Result.success(it.toObjects(Event::class.java))) }
        }
        awaitClose { listener.remove() }
    }

    private fun createGeoQueryFlow(filters: EventFilters, location: Location): Flow<Result<List<Event>>> = callbackFlow {
        val geoQuery: GeoQuery = geoFirestore.queryAtLocation(
            GeoPoint(location.latitude, location.longitude), filters.radiusInKm!!
        )
        val eventsInRadius = mutableMapOf<String, Event>()

        geoQuery.addGeoQueryEventListener(object : GeoQueryEventListener {
            override fun onKeyEntered(documentID: String, location: GeoPoint) {
                firestore.collection("events").document(documentID).get()
                    .addOnSuccessListener { document ->
                        document.toObject<Event>()?.let { event ->
                            if (matchesClientSideFilters(event, filters)) {
                                eventsInRadius[documentID] = event
                                val sortedList = eventsInRadius.values.sortedBy { it.eventTimestamp }
                                trySend(Result.success(sortedList))
                            }
                        }
                    }
            }

            override fun onKeyExited(documentID: String) {
                if (eventsInRadius.remove(documentID) != null) {
                    val sortedList = eventsInRadius.values.sortedBy { it.eventTimestamp }
                    trySend(Result.success(sortedList))
                }
            }
            override fun onKeyMoved(documentID: String, location: GeoPoint) { /* Ignoriši */ }
            override fun onGeoQueryReady() {
                val sortedList = eventsInRadius.values.sortedBy { it.eventTimestamp }
                trySend(Result.success(sortedList))
            }
            override fun onGeoQueryError(exception: Exception) { trySend(Result.failure(exception)) }
        })

        awaitClose { geoQuery.removeAllListeners() }
    }

    private fun matchesClientSideFilters(event: Event, filters: EventFilters): Boolean {
        val categoryMatch = filters.categories.isEmpty() || EventCategory.fromString(event.category) in filters.categories

        val dateMatch = event.eventTimestamp?.toDate()?.let { eventDate ->
            val startOk = filters.startDate?.let { !it.after(eventDate) } ?: true
            val endOk = filters.endDate?.let { !it.before(eventDate) } ?: true
            startOk && endOk
        } ?: (filters.startDate == null && filters.endDate == null)

        return categoryMatch && dateMatch
    }

    fun getEventById(eventId: String): Flow<Result<Event>> = callbackFlow {
        val documentRef = firestore.collection("events").document(eventId)

        val listener = documentRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }
            val event = snapshot?.toObject<Event>()
            if (event != null) {
                trySend(Result.success(event))
            } else {
                trySend(Result.failure(Exception("Event not found")))
            }
        }
        awaitClose { listener.remove() }
    }
}
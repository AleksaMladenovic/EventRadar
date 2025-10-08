package com.eventradar.data.repository

import android.location.Location
import com.eventradar.data.model.Event
import com.eventradar.data.model.EventCategory
import com.eventradar.data.model.EventFilters
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
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

            suspendCancellableCoroutine<Unit> { continuation ->
                geoFirestore.setLocation(
                    documentRef.id,
                    event.location,
                    object : GeoFirestore.CompletionCallback {
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
                "createdAt" to Timestamp.now(),
                "creatorId" to event.creatorId,
                "creatorName" to event.creatorName,
                "eventTimestamp" to event.eventTimestamp,
                "ageRestriction" to event.ageRestriction,
                "price" to event.price,
                "free" to event.free,
                "eventImageUrl" to event.eventImageUrl,
            )

            // Koristimo 'update' da dodamo ostatak podataka bez pregaženja 'g' i 'l'
            documentRef.update(eventData).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getFilteredEvents(userId: String? = null): Flow<Result<List<Event>>> {
        val locationFlow = locationRepository.getLocationUpdates()
            .distinctUntilChanged { old, new -> old.distanceTo(new) < 100f }

        return filterRepository.filters.flatMapLatest { filters ->
            if (filters.radiusInKm != null) {
                locationFlow.flatMapLatest { location ->
                    println("FILTER_DEBUG: Radius filter is active. Using GEO QUERY.")
                    createGeoQueryFlow(filters, location, userId)
                }
            } else {
                println("FILTER_DEBUG: Radius filter is NOT active. Using NORMAL QUERY.")
                createNormalQueryFlow(filters, userId)
            }
        }
    }

    private fun createNormalQueryFlow(filters: EventFilters, userId: String? = null): Flow<Result<List<Event>>> = callbackFlow {
        var query: Query = firestore.collection("events")
        if (!userId.isNullOrBlank()) {
            query = query.whereEqualTo("creatorId", userId)
        }
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createGeoQueryFlow(filters: EventFilters, location: Location, userId: String? = null): Flow<Result<List<Event>>> = callbackFlow {
        val geoQuery: GeoQuery = geoFirestore.queryAtLocation(
            GeoPoint(location.latitude, location.longitude),
            filters.radiusInKm!!
        )

        var firestoreListener: ListenerRegistration? = null
        // Mapa koja čuva ID-jeve svih dokumenata koji su trenutno u radijusu
        val documentIdsInRadius = mutableSetOf<String>()

        fun updateFirestoreListener() {
            // Ukloni starog Firestore listener-a pre nego što postaviš novog
            firestoreListener?.remove()

            // Ako nema događaja u radijusu, odmah pošalji praznu listu
            if (documentIdsInRadius.isEmpty()) {
                trySend(Result.success(emptyList()))
                return
            }

            // Kreiraj NOVI Firestore upit sa 'whereIn' na AŽURIRANIM ID-jevima
            firestoreListener = firestore.collection("events")
                .whereIn(FieldPath.documentId(), documentIdsInRadius.toList()) // .toList() je važno
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val events = snapshot.toObjects<Event>()
                        val filteredEvents = events.filter { event ->
                            matchesClientSideFilters(event, filters, userId)
                        }
                        val sortedList = filteredEvents.sortedBy { it.eventTimestamp }
                        trySend(Result.success(sortedList))
                    }
                }
        }

        // Glavni listener za GeoQuery
        geoQuery.addGeoQueryEventListener(object : GeoQueryEventListener {
            override fun onKeyEntered(documentID: String, location: GeoPoint) {
                // Dodaj novi ID i osveži Firestore listener
                documentIdsInRadius.add(documentID)
                updateFirestoreListener()
            }

            override fun onKeyExited(documentID: String) {
                // Ukloni ID i osveži Firestore listener
                documentIdsInRadius.remove(documentID)
                updateFirestoreListener()
            }

            // Ove metode sada ne moraju ništa da rade
            override fun onKeyMoved(documentID: String, location: GeoPoint) {}
            override fun onGeoQueryReady() {
                // Kada je GeoQuery spreman, možemo da pokrenemo prvi Firestore listener
                updateFirestoreListener()

            }
            override fun onGeoQueryError(exception: Exception) {
                trySend(Result.failure(exception))
            }
        })

        // Kada se Flow otkaže, ukloni oba listener-a
        awaitClose {
            firestoreListener?.remove()
            geoQuery.removeAllListeners()
        }
    }


    private fun matchesClientSideFilters(event: Event, filters: EventFilters,userId: String? = null): Boolean {
        val categoryMatch = filters.categories.isEmpty() || EventCategory.fromString(event.category) in filters.categories

        val dateMatch = event.eventTimestamp?.toDate()?.let { eventDate ->
            val startOk = filters.startDate?.let { !it.after(eventDate) } ?: true
            val endOk = filters.endDate?.let { !it.before(eventDate) } ?: true
            startOk && endOk
        } ?: (filters.startDate == null && filters.endDate == null)

        val userMatch = userId.isNullOrBlank() || event.creatorId == userId

        return categoryMatch && dateMatch && userMatch
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

    fun getEventsByCreator(userId: String): Flow<Result<List<Event>>> = callbackFlow {
        val listener = firestore.collection("events")
            .whereEqualTo("creatorId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(Result.success(it.toObjects(Event::class.java)))
                }
            }
        awaitClose { listener.remove() }
    }


    suspend fun updateEvent(event: Event): Result<Unit> {
        return try {
            firestore.collection("events").document(event.id)
                .set(event, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            firestore.collection("events").document(eventId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
package com.eventradar.data.repository

import android.location.Location
import com.eventradar.data.model.Event
import com.eventradar.data.model.EventCategory
import com.eventradar.data.model.EventFilters
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import org.imperiumlabs.geofirestore.GeoQuery
import org.imperiumlabs.geofirestore.listeners.GeoQueryEventListener
import kotlin.String
import kotlin.coroutines.resume

@Singleton
class EventRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val geoFirestore: GeoFirestore,
    private val filterRepository: FilterRepository,
    private val locationRepository: LocationRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
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
            if (event.creatorId.isNotBlank()) {
                userRepository.incrementUserPoints(event.creatorId, 10L) // +10 poena
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getFilteredEvents(
        createdByUserId: String? = null,
        attendingUserId: String? = null,
    ): Flow<Result<List<Event>>> {
        val locationFlow = locationRepository.getLocationUpdates()
            .distinctUntilChanged { old, new -> old.distanceTo(new) < 100f }

        return filterRepository.filters.flatMapLatest { filters ->
            if (filters.radiusInKm != null) {
                locationFlow.flatMapLatest { location ->
                    println("FILTER_DEBUG: Radius filter is active. Using GEO QUERY.")
                    createGeoQueryFlow(filters, location, createdByUserId, attendingUserId)
                }
            } else {
                println("FILTER_DEBUG: Radius filter is NOT active. Using NORMAL QUERY.")
                createNormalQueryFlow(filters, createdByUserId, attendingUserId)
            }
        }
    }

    private fun createNormalQueryFlow(
        filters: EventFilters,
        createdByUserId: String? = null,
        attendingUserId: String? = null,
        ): Flow<Result<List<Event>>> = callbackFlow {
        var query: Query = firestore.collection("events")
        if (!createdByUserId.isNullOrBlank()) {
            query = query.whereEqualTo("creatorId", createdByUserId)
        }
        if (!attendingUserId.isNullOrBlank()) {
            query = query.whereArrayContains("attendeeIds", attendingUserId)
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
    private fun createGeoQueryFlow(
        filters: EventFilters,
        location: Location,
        createdByUserId: String? = null,
        attendingUserId: String? = null,
    ): Flow<Result<List<Event>>> = callbackFlow {
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
                            matchesClientSideFilters(event, filters, createdByUserId, attendingUserId)
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


    private fun matchesClientSideFilters(
        event: Event,
        filters: EventFilters,
        createdByUserId: String? = null,
        attendingUserId: String? = null,
    ): Boolean {
        val categoryMatch = filters.categories.isEmpty() || EventCategory.fromString(event.category) in filters.categories

        val dateMatch = event.eventTimestamp?.toDate()?.let { eventDate ->
            val startOk = filters.startDate?.let { !it.after(eventDate) } ?: true
            val endOk = filters.endDate?.let { !it.before(eventDate) } ?: true
            startOk && endOk
        } ?: (filters.startDate == null && filters.endDate == null)

        val creatorMatch = createdByUserId.isNullOrBlank() || event.creatorId == createdByUserId
        val attendingMatch = attendingUserId.isNullOrBlank() || attendingUserId in event.attendeeIds
        return categoryMatch && dateMatch && creatorMatch && attendingMatch
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

    suspend fun toggleAttendance(eventId: String, userId: String): Result<Unit> {
        return try {
            val eventRef = firestore.collection("events").document(eventId)
            val userRef = firestore.collection("users").document(userId)

            firestore.runTransaction { transaction ->
                val eventSnapshot = transaction.get(eventRef)
                val currentAttendees = eventSnapshot.get("attendeeIds") as? List<String> ?: emptyList()

                // Proveravamo da li korisnik već prisustvuje
                if (userId in currentAttendees) {
                    // Ako da, ukloni ga (atomic operation)
                    transaction.update(eventRef, "attendeeIds", FieldValue.arrayRemove(userId))
                    transaction.update(userRef, "attendingEventIds", FieldValue.arrayRemove(eventId))
                } else {
                    // Ako ne, dodaj ga (atomic operation)
                    transaction.update(eventRef, "attendeeIds", FieldValue.arrayUnion(userId))
                    transaction.update(userRef, "attendingEventIds", FieldValue.arrayUnion(eventId))
                }
                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun rateEvent(eventId: String, newRating: Double): Result<Unit> {
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            return Result.failure(Exception("User not authenticated."))
        }
        if (newRating < 1.0 || newRating > 5.0) {
            return Result.failure(IllegalArgumentException("Rating must be between 1 and 5."))
        }

        return try {
            val eventRef = firestore.collection("events").document(eventId)

            firestore.runTransaction { transaction ->
                val eventSnapshot = transaction.get(eventRef)
                if (!eventSnapshot.exists()) {
                    throw Exception("Event not found.")
                }

                val currentRatingSum = eventSnapshot.getDouble("ratingSum") ?: 0.0
                val currentRatingCount = eventSnapshot.getLong("ratingCount") ?: 0L
                val ratedByMap = eventSnapshot.get("ratedByUserIds") as? Map<String, Double> ?: emptyMap()

                val oldRating = ratedByMap[userId] // Proveri da li je korisnik već ocenio

                var newRatingSum = currentRatingSum
                var newRatingCount = currentRatingCount

                if (oldRating != null) {
                    // Korisnik menja ocenu
                    newRatingSum = currentRatingSum - oldRating + newRating
                } else {
                    // Korisnik ocenjuje prvi put
                    newRatingSum = currentRatingSum + newRating
                    newRatingCount = currentRatingCount + 1

                    val userRef = firestore.collection("users").document(userId)
                    transaction.update(userRef, "points", FieldValue.increment(3L))
                }

                // Ažuriraj mapu sa novom ocenom korisnika
                val newRatedByMap = ratedByMap.toMutableMap().apply { this[userId] = newRating }

                // Ažuriraj dokument unutar transakcije
                transaction.update(eventRef, "ratingSum", newRatingSum)
                transaction.update(eventRef, "ratingCount", newRatingCount)
                transaction.update(eventRef, "ratedByUserIds", newRatedByMap)

            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEventsNearby(location: Location, radiusInKm: Double): Result<List<Event>> {
        val geoQuery: GeoQuery = geoFirestore.queryAtLocation(
            GeoPoint(location.latitude, location.longitude),
            radiusInKm
        )

        // Koristimo suspendCancellableCoroutine da sačekamo rezultat
        return suspendCancellableCoroutine { continuation ->
            val eventsNearby = mutableListOf<Event>()
            val listener = geoQuery.addGeoQueryEventListener(object : GeoQueryEventListener {
                override fun onKeyEntered(documentID: String, location: GeoPoint) {
                    firestore.collection("events").document(documentID).get()
                        .addOnSuccessListener { document ->
                            document.toObject<Event>()?.let { event ->
                                eventsNearby.add(event)
                            }
                        }
                }

                override fun onGeoQueryReady() {
                    // Kada je upit gotov, vrati rezultat
                    continuation.resume(Result.success(eventsNearby.distinctBy { it.id }))
                    geoQuery.removeAllListeners() // Očisti listener
                }

                override fun onGeoQueryError(exception: Exception) {
                    continuation.resume(Result.failure(exception))
                    geoQuery.removeAllListeners()
                }

                // Ignorišemo ostale
                override fun onKeyExited(documentID: String) {}
                override fun onKeyMoved(documentID: String, location: GeoPoint) {}
            })
        }
    }

}
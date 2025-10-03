package com.eventradar.data.repository

import android.location.Location
import com.eventradar.data.model.Event
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
        // Kombinujemo filtere i lokaciju.
        // distinctUntilChanged() je optimizacija koja sprečava ponovno izvršavanje
        // ako se filteri ili lokacija nisu suštinski promenili.
        return combine(
            filterRepository.filters,
            // Koristimo 'startWith' da Flow lokacije odmah emituje null,
            // što će pokrenuti inicijalni upit bez čekanja na prvu GPS lokaciju.
            locationRepository.getLocationUpdates().map<Location?, Location?>( { it } ).onStart { emit(null) }
        ) { filters, location ->
            println("FILTER_DEBUG: COMBINE triggered. Filters: radius=${filters.radiusInKm}km, category=${filters.category}. Location: ${location?.latitude}")
            Pair(filters, location)
        }.distinctUntilChanged().flatMapLatest { (filters, location) ->
            // Ako je filter za radijus aktivan i imamo lokaciju, radi geo-upit
            if (filters.radiusInKm != null && location != null) {
                println("FILTER_DEBUG: ---> Entering GEO QUERY mode. Center: ${location.latitude}, Radius: ${filters.radiusInKm}km")
                createGeoQueryFlow(filters, location)
            } else {
                // U suprotnom, radi običan upit (samo sa filterom za kategoriju)
                println("FILTER_DEBUG: ---> Entering NORMAL QUERY mode.")

                createNormalQueryFlow(filters)
            }
        }
    }

    // Pomoćna funkcija za OBIČAN upit (filtriranje samo po kategoriji)
    private fun createNormalQueryFlow(filters: EventFilters): Flow<Result<List<Event>>> = callbackFlow {
        var query: Query = firestore.collection("events")

        // Dodaj filter po kategoriji ako je izabrana
        filters.category?.let {
            query = query.whereEqualTo("category", it.name)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val events = snapshot.toObjects(Event::class.java)
                trySend(Result.success(events))
            }
        }
        awaitClose { listener.remove() }
    }

    // Pomoćna funkcija za GEO-UPIT (filtriranje po radijusu i kategoriji)
    private fun createGeoQueryFlow(filters: EventFilters, location: android.location.Location): Flow<Result<List<Event>>> = callbackFlow {
        // Kreiramo geo-upit sa centrom na lokaciji korisnika i radijusom iz filtera
        val geoQuery: GeoQuery = geoFirestore.queryAtLocation(
            GeoPoint(location.latitude, location.longitude),
            filters.radiusInKm!! // Sigurni smo da nije null zbog logike u getFilteredEvents
        )

        // Mapa koja čuva sve događaje koji su trenutno unutar radijusa
        val eventsInRadius = mutableMapOf<String, Event>()

        val listener = geoQuery.addGeoQueryEventListener(object : GeoQueryEventListener {
            override fun onKeyEntered(documentID: String, location: GeoPoint) {
                println("FILTER_DEBUG: onKeyEntered: Found key in radius: $documentID")
                // Događaj je UŠAO u radijus. Sada moramo da dobavimo njegove pune podatke.
                firestore.collection("events").document(documentID).get()
                    .addOnSuccessListener { document ->
                        document.toObject<Event>()?.let { event ->
                            // Dodatno proveravamo da li se poklapa i sa filterom za kategoriju
                            if (filters.category == null || event.category == filters.category.name) {
                                eventsInRadius[documentID] = event
                                // Emituj ažuriranu listu
                                trySend(Result.success(eventsInRadius.values.toList()))
                            }
                        }
                    }
            }

            override fun onKeyExited(documentID: String) {
                // Događaj je IZAŠAO iz radijusa. Ukloni ga iz mape.
                if (eventsInRadius.containsKey(documentID)) {
                    eventsInRadius.remove(documentID)
                    trySend(Result.success(eventsInRadius.values.toList()))
                }
            }

            override fun onKeyMoved(documentID: String, location: GeoPoint) {
                // Ignorišemo za sada, ali ovo se desi ako se lokacija postojećeg događaja promeni
            }

            override fun onGeoQueryReady() {
                println("FILTER_DEBUG: GeoQuery is ready. Total events in radius: ${eventsInRadius.size}")
                // Svi početni događaji su učitani. Možemo poslati inicijalno stanje.
                println("GeoQuery is ready. Found ${eventsInRadius.size} events.")
                trySend(Result.success(eventsInRadius.values.toList()))
            }

            override fun onGeoQueryError(exception: Exception) {
                println("FILTER_DEBUG: GEO QUERY FAILED: ${exception.message}")
                // Greška u geo-upitu
                trySend(Result.failure(exception))
            }
        })

        // Kada se Flow otkaže, ukloni GeoQuery listener
        awaitClose {
            geoQuery.removeAllListeners()
        }
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
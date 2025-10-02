package com.eventradar.data.repository

import com.eventradar.data.model.Event
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun addEvent(event: Event): Result<Unit> {
        return try {
            val documentRef = firestore.collection("events").document()

            firestore.collection("events").document(documentRef.id)
                .set(event.copy(id = documentRef.id)).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllEvents(): Flow<Result<List<Event>>> = callbackFlow{
        val collection = firestore.collection("events")
        val snapshotListener = collection.addSnapshotListener { snapshot, error->
            if(error!=null){
                trySend(Result.failure(error))
                return@addSnapshotListener
            }
            if(snapshot!=null){
                val events = snapshot.toObjects<Event>()
                trySend(Result.success(events))
            }
        }

        awaitClose {
            snapshotListener.remove()
        }
    }
}
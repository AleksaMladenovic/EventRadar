package com.eventradar.data.repository

import com.eventradar.data.model.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    // Funkcija za dodavanje novog komentara
    suspend fun addComment(eventId: String, text: String): Result<Unit> {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            return Result.failure(Exception("User not authenticated"))
        }
        if (text.isBlank()) {
            return Result.failure(Exception("Comment cannot be empty"))
        }

        return try {
            val documentRef = firestore.collection("comments").document()
            val newComment = Comment(
                id = documentRef.id,
                eventId = eventId,
                authorId = currentUserId,
                text = text
            )
            documentRef.set(newComment).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Funkcija za dobavljanje svih komentara za događaj u realnom vremenu
    fun getCommentsForEvent(eventId: String): Flow<Result<List<Comment>>> = callbackFlow {
        if (eventId.isBlank()) {
            trySend(Result.success(emptyList()))
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore.collection("comments")
            .whereEqualTo("eventId", eventId)
            .orderBy("timestamp", Query.Direction.ASCENDING) // Prikazujemo od najstarijeg ka najnovijem
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val comments = snapshot.toObjects<Comment>()
                    trySend(Result.success(comments))
                }
            }

        // Kada se Flow otkaže, ukloni listener-a
        awaitClose { listener.remove() }
    }
}

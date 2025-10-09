package com.eventradar.data.repository

import android.location.Location
import android.net.Uri
import com.eventradar.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val cloudinaryRepository: CloudinaryRepository
) {
    suspend fun getCurrentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            firestore.collection("users").document(uid).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createUserProfile(
        firebaseUser: FirebaseUser,
        firstName: String,
        lastName: String,
        username: String,
        phone: String,
        profileImageUri: Uri?
    ): Result<Unit> {
        return try {
            val uid = firebaseUser.uid

            // Logika za upload slike sada živi ovde
            val profileImageUrl = if (profileImageUri != null) {
                cloudinaryRepository.uploadProfileImage(profileImageUri).getOrNull()
            } else {
                null
            }

            val newUser = User(
                uid = uid,
                firstName = firstName,
                lastName = lastName,
                username = username,
                phone = phone,
                email = firebaseUser.email ?: "",
                profileImageUrl = profileImageUrl
            )

            // Sačuvaj User objekat u Firestore
            firestore.collection("users").document(uid).set(newUser).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllUsersSortedByPoints(): Flow<Result<List<User>>> = callbackFlow {
        val listener = firestore.collection("users")
            .orderBy("points", Query.Direction.DESCENDING) // Sortiraj po poenima opadajuće
            .limit(100) // Opciono: Ograniči na top 100 korisnika
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val users = snapshot.toObjects<User>()
                    trySend(Result.success(users))
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun incrementUserPoints(userId: String, pointsToAdd: Long): Result<Unit> {
        return try {
            val userRef = firestore.collection("users").document(userId)
            userRef.update("points", FieldValue.increment(pointsToAdd)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserById(userId: String): Result<User?> {
        return try {
            val document = firestore.collection("users").document(userId).get().await()
            Result.success(document.toObject(User::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserLocation(userId: String, location: Location): Result<Unit> {
        return try {
            val userRef = firestore.collection("users").document(userId)
            // Ažuriramo samo jedno polje - 'lastKnownLocation'
            userRef.update("lastKnownLocation", GeoPoint(location.latitude, location.longitude)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun doesUsernameExist(username: String): Boolean {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
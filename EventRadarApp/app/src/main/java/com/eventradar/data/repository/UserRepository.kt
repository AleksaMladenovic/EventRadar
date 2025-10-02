package com.eventradar.data.repository

import android.net.Uri
import com.eventradar.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
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
}
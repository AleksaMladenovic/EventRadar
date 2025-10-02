package com.eventradar.data.repository

import android.net.Uri
import com.eventradar.data.model.User
import com.eventradar.ui.auth.AuthState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storageRepository: StorageRepository
)  {

    fun getAuthStateFlow(): StateFlow<AuthState> {
        val flow = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                flow.value = AuthState.Authenticated
            } else {
                flow.value = AuthState.Unauthenticated
            }
        }
        return flow.asStateFlow()
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        username: String,
        phone: String,
        profileImageUri: Uri?
    ): Result<Unit> {
        return try {
            // Korak 1: Kreiraj korisnika u Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("UID is null after registration.")

            // Korak 2: Upload-uj sliku ako postoji
            val profileImageUrl = if (profileImageUri != null) {
                // Pozivamo StorageRepository
                storageRepository.uploadProfileImage(profileImageUri).getOrNull()
            } else {
                null
            }

            // Korak 3: Kreiraj User objekat sa URL-om slike
            val newUser = User(
                uid = uid,
                firstName = firstName,
                lastName = lastName,
                username = username,
                phone = phone,
                email = email,
                profileImageUrl = profileImageUrl // <-- Dodajemo URL slike
            )

            // Korak 4: Sačuvaj User objekat u Firestore
            firestore.collection("users").document(uid).set(newUser).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
package com.eventradar.data.repository

import com.eventradar.data.model.User
import com.eventradar.ui.auth.AuthState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val auth: FirebaseAuth) {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

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

    // Ovu funkciju ćemo ignorisati za sada, ali neka stoji da ne pravi greške
    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        username: String,
        phone: String
    ): Result<Unit> {
        return try {
            // Korak 1: Kreiraj korisnika u Firebase Authentication
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Firebase user is null after registration.")
            val uid = firebaseUser.uid

            // Korak 2: Kreiraj naš User objekat sa svim podacima
            val newUser = User(
                uid = uid,
                firstName = firstName,
                lastName = lastName,
                username = username,
                phone = phone,
                email = email // Sačuvaj i email u Firestore za lakši pristup
            )

            // Korak 3: Sačuvaj User objekat u Firestore kolekciju "users"
            // Dokument će imati isti ID kao i korisnikov UID
            firestore.collection("users").document(uid).set(newUser).await()

            // Ako je sve prošlo, vrati uspeh
            Result.success(Unit)
        } catch (e: Exception) {
            // Ako bilo šta pukne (kreiranje u Auth ili pisanje u Firestore), vrati grešku
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
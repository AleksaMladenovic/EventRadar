package com.eventradar.data.repository

import android.net.Uri
import com.eventradar.data.model.User
import com.eventradar.ui.auth.AuthState
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
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

    suspend fun register(email: String, password: String): Result<AuthResult> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
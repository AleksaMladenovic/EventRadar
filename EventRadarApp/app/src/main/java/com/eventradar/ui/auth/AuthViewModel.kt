package com.eventradar.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventradar.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository // Koristićemo ga!
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.getAuthStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Počni da slušaš kad se UI pojavi
            initialValue = AuthState.Loading // Početno stanje dok ne dobijemo prvi status
        )

    // Jedina funkcija koja nam treba ovde.
    // Ona samo prosleđuje komandu repozitorijumu.
    fun signOut() {
        authRepository.signOut()
    }
}

// AuthState sealed klasa ostaje ista, možeš je ostaviti u ovom fajlu ili je premestiti
sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState() // Korisno kao početno stanje
    data class Error(val message: String) : AuthState()
}
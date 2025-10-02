package com.eventradar.ui.auth.register

import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventradar.R
import com.eventradar.data.repository.AuthRepository
import com.eventradar.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RegisterEvent {
    object RegisterSuccess : RegisterEvent()
    data class RegisterError(val message: String) : RegisterEvent()
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(RegistrationFormState())
    val formState: StateFlow<RegistrationFormState> = _formState.asStateFlow()

    private val _registerEvent = MutableSharedFlow<RegisterEvent>()
    val registerEvent = _registerEvent.asSharedFlow()
    // --- Funkcije za ažuriranje stanja forme ---
    fun onFirstNameChanged(value: String) {
        _formState.update { it.copy(firstName = value, firstNameError = null) }
    }
    fun onLastNameChanged(value: String) {
        _formState.update { it.copy(lastName = value, lastNameError = null) }
    }
    fun onUsernameChanged(value: String) {
        _formState.update { it.copy(username = value, usernameError = null) }
    }
    fun onPhoneChanged(value: String) {
        _formState.update { it.copy(phone = value, phoneError = null) }
    }
    fun onEmailChanged(value: String) {
        _formState.update { it.copy(email = value, emailError = null) }
    }
    fun onPasswordChanged(value: String) {
        _formState.update { it.copy(password = value, passwordError = null) }
    }

    // NOVO: Funkcija za promenu slike
    fun onProfileImageChanged(uri: Uri?) {
        println("VIEWMODEL: onProfileImageChanged called with URI: $uri")
        _formState.update { it.copy(profileImageUri = uri) }
    }

    fun register() {
        viewModelScope.launch {
            if (!validateForm()) {
                return@launch // Ako forma nije validna, prekini izvršavanje
            }

            _formState.update { it.copy(isLoading = true) }
            val state = _formState.value

            // KORAK 1: Kreiraj nalog u Firebase Auth
            // 'authRepository.register' JE SUSPEND FUNKCIJA, I POZIVAMO JE UNUTAR KORUTINE
            val authResult = authRepository.register(state.email, state.password)

            // Obrada rezultata
            authResult.onSuccess { result ->
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    // KORAK 2: Ako je Auth uspeo, kreiraj profil u Firestore
                    // 'userRepository.createUserProfile' JE TAKOĐE SUSPEND FUNKCIJA
                    val profileResult = userRepository.createUserProfile(
                        firebaseUser = firebaseUser,
                        firstName = state.firstName,
                        lastName = state.lastName,
                        username = state.username,
                        phone = state.phone,
                        profileImageUri = state.profileImageUri
                    )

                    _formState.update { it.copy(isLoading = false) }

                    if (profileResult.isSuccess) {
                        _registerEvent.emit(RegisterEvent.RegisterSuccess)
                    } else {
                        val error = profileResult.exceptionOrNull()?.message ?: "Error creating user profile."
                        _registerEvent.emit(RegisterEvent.RegisterError(error))
                    }
                } else {
                    // Slučaj ako je Firebase vratio uspeh, ali je user objekat null (retko)
                    _formState.update { it.copy(isLoading = false) }
                    _registerEvent.emit(RegisterEvent.RegisterError("Failed to get user info after registration."))
                }
            }.onFailure { exception ->
                // Greška pri kreiranju naloga u Firebase Auth
                _formState.update { it.copy(isLoading = false) }
                _registerEvent.emit(RegisterEvent.RegisterError(exception.message ?: "An unknown error occurred."))
            }
        }
    }


    private fun validateForm(): Boolean {
        val state = _formState.value

        // Vršimo provere za svako polje
        val firstNameError = if (state.firstName.isBlank()) R.string.error_field_required else null
        val lastNameError = if (state.lastName.isBlank()) R.string.error_field_required else null
        val usernameError = if (state.username.isBlank()) R.string.error_field_required else null
        val phoneError = if (state.phone.isBlank()) R.string.error_field_required else null
        val emailError = if (!Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) R.string.error_invalid_email else null
        val passwordError = if (state.password.length < 6) R.string.error_password_too_short else null

        // Ažuriramo stanje sa svim greškama odjednom
        _formState.update {
            it.copy(
                firstNameError = firstNameError,
                lastNameError = lastNameError,
                usernameError = usernameError,
                phoneError = phoneError,
                emailError = emailError,
                passwordError = passwordError
            )
        }

        // Forma je validna ako nijedno polje za grešku nije postavljeno
        return listOfNotNull(firstNameError, lastNameError, usernameError, phoneError, emailError, passwordError).isEmpty()
    }
}
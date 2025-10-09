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
            _formState.update { it.copy(isLoading = true) }

            if(validateForm()){
                val state = _formState.value
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
                        _formState.update { it.copy(isLoading = false) }
                        // Slučaj ako je Firebase vratio uspeh, ali je user objekat null (retko)
                        _registerEvent.emit(RegisterEvent.RegisterError("Failed to get user info after registration."))
                    }

                }.onFailure { exception ->
                    _formState.update { it.copy(isLoading = false) }
                    // Greška pri kreiranju naloga u Firebase Auth
                    _registerEvent.emit(RegisterEvent.RegisterError(exception.message ?: "An unknown error occurred."))
                }

            }
        }
    }


    private suspend fun validateForm(): Boolean {
        val state = _formState.value
        val phoneRegex = Regex("^\\+?[0-9]+\$")

        // 1. Sinhrone provere
        val firstNameError = if (state.firstName.isBlank()) R.string.error_field_required else null
        val lastNameError = if (state.lastName.isBlank()) R.string.error_field_required else null
        val usernameError = if (state.username.length < 3) R.string.error_username_too_short else null
        val phoneError = if (!state.phone.matches(phoneRegex)) R.string.error_invalid_phone else null
        val emailError = if (!Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) R.string.error_invalid_email else null
        val passwordError = if (state.password.length < 6) R.string.error_password_too_short else null

        val hasSyncError = listOfNotNull(firstNameError, lastNameError, usernameError, phoneError, emailError, passwordError).isNotEmpty()

        // Ako ima sinhronih grešaka, odmah ažuriraj stanje i vrati 'false'
        if (hasSyncError) {
            _formState.update {
                it.copy(
                    firstNameError = firstNameError,
                    lastNameError = lastNameError,
                    usernameError = usernameError,
                    phoneError = phoneError,
                    emailError = emailError,
                    passwordError = passwordError,
                    isLoading = false,
                )
            }
            return false
        }

        // 2. Asinhrona provera (ako su sinhrone prošle)
        if (userRepository.doesUsernameExist(state.username)) {
            _formState.update {
                it.copy(usernameError = R.string.error_username_taken, isLoading = false)
            }
            return false // Pronađen je korisnik sa istim imenom
        }

        // Ako je sve prošlo, forma je validna
        return true
    }
}
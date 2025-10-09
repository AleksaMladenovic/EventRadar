package com.eventradar.ui.edit_profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventradar.R
import com.eventradar.data.repository.AuthRepository
import com.eventradar.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    private var originalUsername: String? = null

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            if (user != null) {
                originalUsername = user.username
                _state.update {
                    it.copy(
                        isLoading = false,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        username = user.username,
                        phone = user.phone
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false, error = "Failed to load user data.") }
            }
        }
    }

    fun onFirstNameChanged(name: String) {
        _state.update { it.copy(firstName = name, firstNameError = null) }
    }
    fun onLastNameChanged(name: String) {
        _state.update { it.copy(lastName = name, lastNameError = null) }
    }
    fun onUsernameChanged(username: String) {
        _state.update { it.copy(username = username, usernameError = null) }
    }
    fun onPhoneChanged(phone: String) {
        _state.update { it.copy(phone = phone, phoneError = null) }
    }

    fun onSaveClicked() {
        viewModelScope.launch {
            // Prvo, pokreni validaciju
            if (validateForm()) {
                _state.update { it.copy(isLoading = true) }

                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _state.update { it.copy(isLoading = false, error = "Authentication error.") }
                    return@launch
                }

                val currentState = _state.value
                val result = userRepository.updateUserProfile(
                    userId = userId,
                    firstName = currentState.firstName,
                    lastName = currentState.lastName,
                    username = currentState.username,
                    phone = currentState.phone
                )

                if (result.isSuccess) {
                    _state.update { it.copy(isLoading = false, saveSuccess = true) }
                } else {
                    _state.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
                }
            }
        }
    }

    private suspend fun validateForm(): Boolean {
        val state = _state.value
        val phoneRegex = Regex("^\\+?[0-9]+\$")

        val firstNameError = if (state.firstName.isBlank()) R.string.error_field_required else null
        val lastNameError = if (state.lastName.isBlank()) R.string.error_field_required else null
        val usernameError = if (state.username.length < 3) R.string.error_username_too_short else null
        val phoneError = if (!state.phone.matches(phoneRegex)) R.string.error_invalid_phone else null

        val hasSyncError = listOfNotNull(firstNameError, lastNameError, usernameError, phoneError).isNotEmpty()

        // Ažuriraj stanje sa sinhronim greškama
        _state.update {
            it.copy(
                firstNameError = firstNameError,
                lastNameError = lastNameError,
                usernameError = usernameError,
                phoneError = phoneError
            )
        }

        if (hasSyncError) return false

        // Asinhrona provera: Proveri username samo ako je promenjen!
        if (state.username != originalUsername && userRepository.doesUsernameExist(state.username)) {
            _state.update { it.copy(usernameError = R.string.error_username_taken) }
            return false
        }

        return true
    }
}

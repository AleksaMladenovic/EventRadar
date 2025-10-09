package com.eventradar.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventradar.R // Importuj R fajl za string resurse
import com.eventradar.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Događaji koje ViewModel šalje UI-u
sealed class LoginEvent {
    object LoginSuccess : LoginEvent()
    data class LoginError(val message: String) : LoginEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher
): ViewModel() {

    private val _formState = MutableStateFlow(LoginFormState())
    val formState: StateFlow<LoginFormState> = _formState.asStateFlow()

    private val _loginEvent = MutableSharedFlow<LoginEvent>()
    val loginEvent = _loginEvent.asSharedFlow()

    fun onEmailChanged(email: String) {
        // Kada korisnik kuca, brišemo grešku za to polje
        _formState.update { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChanged(password: String) {
        _formState.update { it.copy(password = password, passwordError = null) }
    }

    fun login() {
        viewModelScope.launch(ioDispatcher) {
            val state = _formState.value

            // --- VALIDACIJA ---
            val isEmailEmpty = state.email.isBlank()
            val isPasswordEmpty = state.password.isBlank()

            if (isEmailEmpty || isPasswordEmpty) {
                // Ažuriraj stanje forme sa porukama o greškama
                _formState.update {
                    it.copy(
                        emailError = if (isEmailEmpty) R.string.error_field_required else null,
                        passwordError = if (isPasswordEmpty) R.string.error_field_required else null
                    )
                }
                return@launch
            }

            // --- POZIV REPOSITORY-JA ---
            _formState.update { it.copy(isLoading = true) }

            // ViewModel više ne zna za Firebase! Samo zove Repository.
            val result = authRepository.login(state.email, state.password)

            _formState.update { it.copy(isLoading = false) }

            // Obrada rezultata koji je vratio Repository
            if (result.isSuccess) {
                _loginEvent.emit(LoginEvent.LoginSuccess)
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown login error"
                _loginEvent.emit(LoginEvent.LoginError(errorMessage))
            }
        }
    }
}
package com.eventradar.ui.change_password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventradar.R
import com.eventradar.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordState())
    val state: StateFlow<ChangePasswordState> = _state.asStateFlow()

    fun onOldPasswordChanged(password: String) {
        _state.update { it.copy(oldPassword = password, error = null) }
    }
    fun onNewPasswordChanged(password: String) {
        _state.update { it.copy(newPassword = password, error = null) }
    }
    fun onConfirmNewPasswordChanged(password: String) {
        _state.update { it.copy(confirmNewPassword = password, error = null) }
    }

    fun onChangePasswordClicked() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val currentState = _state.value

            // Validacija
            if (currentState.newPassword.length < 6) {
                _state.update { it.copy(isLoading = false, error = R.string.error_password_too_short) }
                return@launch
            }
            if (currentState.newPassword != currentState.confirmNewPassword) {
                _state.update { it.copy(isLoading = false, error = R.string.error_passwords_do_not_match) }
                return@launch
            }

            val result = authRepository.changePassword(currentState.oldPassword, currentState.newPassword)

            if (result.isSuccess) {
                _state.update { it.copy(isLoading = false, success = true) }
            } else {
                _state.update { it.copy(isLoading = false, error = R.string.error_incorrect_password) }
            }
        }
    }
}

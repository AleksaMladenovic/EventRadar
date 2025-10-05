package com.eventradar.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventradar.data.repository.AuthRepository
import com.eventradar.data.repository.EventRepository
import com.eventradar.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val user = userRepository.getCurrentUser()
            if (user != null) {
                _state.update { it.copy(user = user, isLoading = false, error = null) }
                loadMyEvents(user.uid)
            } else {
                _state.update { it.copy(isLoading = false, error = "Failed to load user profile.") }
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    private fun loadMyEvents(userId: String) {
        viewModelScope.launch {
            eventRepository.getEventsByCreator(userId)
                .onEach { result ->
                    result.onSuccess { events ->
                        _state.update { it.copy(myEvents = events) }
                    }
                }
        }
    }
}

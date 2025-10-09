package com.eventradar.ui.profile

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventradar.data.repository.AuthRepository
import com.eventradar.data.repository.EventRepository
import com.eventradar.data.repository.UserRepository
import com.eventradar.services.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val application: Application,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val eventRepository: EventRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch(ioDispatcher) {
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
        Intent(application, LocationService::class.java).also {
            it.action = LocationService.ACTION_STOP
        application.startService(it)
        }
        authRepository.signOut()
    }

    private fun loadMyEvents(userId: String) {

            eventRepository.getEventsByCreator(userId)
                .flowOn(ioDispatcher)
                .onEach { result ->
                    result.onSuccess { events ->
                        _state.update { it.copy(myEvents = events) }
                    }
                }.launchIn(viewModelScope)

    }
}

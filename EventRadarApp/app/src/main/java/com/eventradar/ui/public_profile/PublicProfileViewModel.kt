package com.eventradar.ui.public_profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventradar.data.repository.EventRepository
import com.eventradar.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val eventRepository: EventRepository,
    private val ioDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(PublicProfileState())
    val state: StateFlow<PublicProfileState> = _state.asStateFlow()

    init {
        val userId: String? = savedStateHandle.get("userId")

        if (userId != null) {
            // Pokrećemo dobavljanje podataka za korisnika i njegove događaje
            loadUserProfile(userId)
            loadUserEvents(userId)
        } else {
            _state.update { it.copy(isLoading = false, error = "User ID not provided.") }
        }
    }

    private fun loadUserProfile(userId: String) {
        viewModelScope.launch(ioDispatcher) {
            _state.update { it.copy(isLoading = true) }
            val result = userRepository.getUserById(userId)
            result.onSuccess { user ->
                _state.update { it.copy(user = user, isLoading = false) }
            }.onFailure { exception ->
                _state.update { it.copy(error = exception.message, isLoading = false) }
            }
        }
    }

    private fun loadUserEvents(userId: String) {
        // Slušamo promene na događajima u realnom vremenu
        eventRepository.getEventsByCreator(userId)
            .flowOn(ioDispatcher)
            .onEach { result ->
                result.onSuccess { events ->
                    _state.update { it.copy(userEvents = events) }
                }.onFailure { exception ->
                    _state.update { it.copy(error = it.error ?: exception.message) }
                }
            }
            .launchIn(viewModelScope)
    }
}

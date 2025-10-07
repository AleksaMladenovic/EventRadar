package com.eventradar.ui.event_details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventradar.data.repository.AuthRepository
import com.eventradar.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class EventDetailsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle // Za primanje eventId iz navigacije
) : ViewModel() {

    private val _state = MutableStateFlow(EventDetailsState())
    val state: StateFlow<EventDetailsState> = _state.asStateFlow()

    init {
        // Čitamo 'eventId' iz argumenata rute
        val eventId: String? = savedStateHandle.get("eventId")

        if (eventId != null) {
            getEventDetails(eventId)
        } else {
            // Slučaj ako ID nije prosleđen - prikaži grešku
            _state.update { it.copy(isLoading = false, error = "Event ID not provided.") }
        }
    }

    private fun getEventDetails(eventId: String) {
        // .onEach se poziva svaki put kada Flow emituje novu vrednost
        eventRepository.getEventById(eventId)
            .onEach { result ->
                result.onSuccess { event ->
                    // Ako je uspešno, ažuriraj stanje sa podacima o događaju
                    _state.update { it.copy(event = event, isLoading = false, error = null) }
                }.onFailure { exception ->
                    // Ako je neuspešno, ažuriraj stanje sa porukom o grešci
                    _state.update { it.copy(isLoading = false, error = exception.message) }
                }
            }
            .launchIn(viewModelScope) // Pokreni sakupljanje (collecting) u ViewModelScope
    }
    fun isCurrentUserOwner(creatorId: String): Boolean {
        return authRepository.getCurrentUserId() == creatorId
    }

}
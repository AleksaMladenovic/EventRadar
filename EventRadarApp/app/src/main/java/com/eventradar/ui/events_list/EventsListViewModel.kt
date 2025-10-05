package com.eventradar.ui.events_list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventradar.data.model.Event
import com.eventradar.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventsListViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(EventsListState())
    val state: StateFlow<EventsListState> = _state.asStateFlow()
    private val userId: String? = savedStateHandle.get("userId")

    init {
        viewModelScope.launch {
            eventRepository.getFilteredEvents(userId).collect { result ->
                result.onSuccess { events ->
                    _state.update {
                        it.copy(
                            serverEvents = events,
                            isLoading = false,
                            error = null,
                            filteredEvents = filterEvents(events, it.searchQuery)
                        )
                    }
                }.onFailure { exception ->
                    _state.update { it.copy(isLoading = false, error = exception.message) }
                }
            }
        }

    }

    fun onSearchQueryChanged(query: String) {
        _state.update {
            // Ažuriraj query i odmah ponovo filtriraj listu
            it.copy(
                searchQuery = query,
                filteredEvents = filterEvents(it.serverEvents, query)
            )
        }
    }

    private fun filterEvents(events: List<Event>, query: String): List<Event> {
        return if (query.isBlank()) {
            events
        } else {
            events.filter { event ->
                event.name.contains(query, ignoreCase = true)
            }
        }
    }


}
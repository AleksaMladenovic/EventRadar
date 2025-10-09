package com.eventradar.ui.events_list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventradar.data.model.Event
import com.eventradar.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventsListViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    savedStateHandle: SavedStateHandle,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(EventsListState())
    val state: StateFlow<EventsListState> = _state.asStateFlow()
    private val createdByUserId: String? = savedStateHandle.get("createdByUserId")
    private val attendingUserId: String? = savedStateHandle.get("attendingUserId")

    init {
        viewModelScope.launch(ioDispatcher) {
            eventRepository.getFilteredEvents(
                createdByUserId = createdByUserId,
                attendingUserId = attendingUserId,
            ).collect { result ->
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
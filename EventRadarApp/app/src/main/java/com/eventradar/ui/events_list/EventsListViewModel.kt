package com.eventradar.ui.events_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class EventsListViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EventsListState())
    val state: StateFlow<EventsListState> = _state.asStateFlow()

    init {
        getEvents()
    }

    private fun getEvents() {
        eventRepository.getAllEvents()
            .onEach { result ->
                result.onSuccess { events ->
                    _state.update { it.copy(events = events, isLoading = false, error = null) }
                }.onFailure { exception ->
                    _state.update { it.copy(isLoading = false, error = exception.message) }
                }
            }
            .launchIn(viewModelScope)
    }
}
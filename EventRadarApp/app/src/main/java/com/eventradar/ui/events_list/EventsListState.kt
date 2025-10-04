package com.eventradar.ui.events_list

import com.eventradar.data.model.Event


data class EventsListState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val serverEvents: List<Event> = emptyList(),
    val filteredEvents: List<Event> = emptyList()
)
package com.eventradar.ui.events_list

import com.eventradar.data.model.Event


data class EventsListState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
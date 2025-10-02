package com.eventradar.ui.event_details

import com.eventradar.data.model.Event

data class EventDetailsState(
    val event: Event? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)
package com.eventradar.ui.map
import android.location.Location
import com.eventradar.data.model.Event

data class MapState(
    val lastKnownLocation: Location? = null,
    val events: List<Event> = emptyList(),
    val isAddEventDialogShown: Boolean = false,
    val addEventError: String? = null
)
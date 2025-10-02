package com.eventradar.ui.map
import android.location.Location
import androidx.annotation.StringRes
import com.eventradar.data.model.Event

data class MapState(
    val lastKnownLocation: Location? = null,
    val events: List<Event> = emptyList(),
    val isInAddEventMode: Boolean = false
)
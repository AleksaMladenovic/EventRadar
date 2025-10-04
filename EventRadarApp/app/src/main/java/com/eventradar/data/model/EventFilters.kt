package com.eventradar.data.model

data class EventFilters(
    val categories: Set<EventCategory> = emptySet(),
    val radiusInKm: Double? = null
)

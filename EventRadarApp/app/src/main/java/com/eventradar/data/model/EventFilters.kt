package com.eventradar.data.model

import java.util.Date

data class EventFilters(
    val categories: Set<EventCategory> = emptySet(),
    val radiusInKm: Double? = null,
    val startDate: Date? = null,
    val endDate: Date? = null
)

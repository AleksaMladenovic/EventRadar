package com.eventradar.data.model

import java.util.Date

data class EventFilters(
    val categories: Set<EventCategory> = emptySet(),
    val radiusInKm: Double? = 10.0,
    val startDate: Date? = Date(),
    val endDate: Date? = null
)

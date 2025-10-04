package com.eventradar.data.repository

import com.eventradar.data.model.EventCategory
import com.eventradar.data.model.EventFilters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterRepository @Inject constructor() {
    private val _filters = MutableStateFlow(EventFilters())
    val filters: StateFlow<EventFilters> = _filters.asStateFlow()

    fun applyFilters(newFilters: EventFilters) {
        _filters.value = newFilters
    }

    fun resetFilters() {
        _filters.value = EventFilters()
    }
}
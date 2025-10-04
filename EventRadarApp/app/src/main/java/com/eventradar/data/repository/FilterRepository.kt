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

    fun toggleCategory(category: EventCategory) {
        _filters.update { currentFilters ->
            val currentCategories = currentFilters.categories.toMutableSet()
            if(category in currentCategories){
                currentCategories.remove(category)
            }else{
                currentCategories.add(category)
            }
            currentFilters.copy(categories = currentCategories)
        }
    }

    fun updateRadius(radius: Double?) {
        println("FILTER_DEBUG: FilterRepository updating radius to: $radius")
        _filters.update { it.copy(radiusInKm = radius) }
    }

    fun resetFilters() {
        _filters.value = EventFilters()
    }
}
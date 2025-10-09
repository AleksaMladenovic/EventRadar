package com.eventradar.ui.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventradar.data.model.EventCategory
import com.eventradar.data.model.EventFilters
import com.eventradar.data.repository.FilterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class FilterViewModel @Inject constructor(
    private val filterRepository: FilterRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _temporaryFilters = MutableStateFlow(filterRepository.filters.value)
    // Javno stanje koje UI (BottomSheet) posmatra
    val temporaryFilters: StateFlow<EventFilters> = _temporaryFilters.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            filterRepository.filters.collect { appliedFilters ->
                _temporaryFilters.value = appliedFilters
            }
        }
    }
    fun onCategoryToggled(category: EventCategory) {
        _temporaryFilters.update { currentFilters ->
            val currentCategories = currentFilters.categories.toMutableSet()
            if (category in currentCategories) currentCategories.remove(category)
            else currentCategories.add(category)
            currentFilters.copy(categories = currentCategories)
        }
    }


    fun onStartDateChanged(date: Date?) {
        _temporaryFilters.update { it.copy(startDate = date) }
    }

    fun onEndDateChanged(date: Date?) {
        _temporaryFilters.update { it.copy(endDate = date) }
    }

    fun onRadiusChange(radius: Float) {
        val radiusInKm = if (radius == MAX_RADIUS) null else radius.toDouble()
        _temporaryFilters.update { it.copy(radiusInKm = radiusInKm) }
    }

    fun applyFilters() {
        filterRepository.applyFilters(_temporaryFilters.value)
    }

    fun resetFilters() {
        // Reset sada postavlja privremene filtere na podrazumevane vrednosti
        _temporaryFilters.value = EventFilters()
    }


    companion object {
        const val MAX_RADIUS = 101f // Jedna vrednost više od maksimuma slidera za "bez limita"
    }
}

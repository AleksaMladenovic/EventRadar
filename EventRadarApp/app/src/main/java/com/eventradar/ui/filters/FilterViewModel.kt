package com.eventradar.ui.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventradar.data.model.EventCategory
import com.eventradar.data.model.EventFilters
import com.eventradar.data.repository.FilterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FilterViewModel @Inject constructor(
    private val filterRepository: FilterRepository
) : ViewModel() {

    // Direktno "preslikavamo" stanje filtera iz repozitorijuma
    val filters: StateFlow<EventFilters> = filterRepository.filters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), filterRepository.filters.value)

    fun onCategoryToggled(category: EventCategory) {
        filterRepository.toggleCategory(category)
    }

    fun onRadiusChange(radius: Float) {
        // Ako je slider na maksimumu, smatramo da nema limita (null)
        val radiusInKm = if (radius == MAX_RADIUS) null else radius.toDouble()
        println("FILTER_DEBUG: onRadiusChange called. New radius (km): $radiusInKm")
        filterRepository.updateRadius(radiusInKm)
    }

    fun onResetFilters() {
        filterRepository.resetFilters()
    }

    companion object {
        const val MAX_RADIUS = 101f // Jedna vrednost više od maksimuma slidera za "bez limita"
    }
}

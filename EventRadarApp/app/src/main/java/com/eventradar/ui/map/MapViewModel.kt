package com.eventradar.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventradar.data.model.Event
import com.eventradar.data.repository.EventRepository
import com.eventradar.data.repository.LocationRepository
import com.eventradar.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val eventRepository: EventRepository,
) : ViewModel() {

    private val _mapState = MutableStateFlow(MapState())
    val mapState: StateFlow<MapState> = _mapState.asStateFlow()
    private var locationJob : Job? = null;

    init {
        fetchEvents()
    }
    fun startLocationUpdates() {
        if(locationJob?.isActive == true) return

        locationJob = viewModelScope.launch {
            locationRepository.getLocationUpdates().cancellable().collect { location ->
                _mapState.update { it.copy(lastKnownLocation = location) }
            }
        }
    }

    fun stopLocationUpdates(){
        locationJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }

    private fun fetchEvents(){
        viewModelScope.launch {
            eventRepository.getAllEvents().collect { result ->
                result.onSuccess { events ->
                    _mapState.update { it.copy(events = events) }
                }.onFailure {
                    println("Error fetching events: ${it.message}")
                }

            }
        }
    }

    fun onEnterAddEventMode() {
        _mapState.update { it.copy(isInAddEventMode = true) }
    }

    fun onExitAddEventMode() {
        _mapState.update { it.copy(isInAddEventMode = false) }
    }

}
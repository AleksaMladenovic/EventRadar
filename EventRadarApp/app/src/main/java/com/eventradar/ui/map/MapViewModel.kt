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
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
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
                    //TODO: Obraditi gresku na primer Toast
                    println("Error fetching events: ${it.message}")
                }

            }
        }
    }

    fun onShowAddEventDialog() {
        _mapState.update { it.copy(isAddEventDialogShown = true, addEventError = null) }
    }

    fun onDismissAddEventDialog() {
        _mapState.update { it.copy(isAddEventDialogShown = false) }
    }

    fun addEvent(name: String, description: String, category: String) {
        viewModelScope.launch {
            // Validacija
            if (name.isBlank() || description.isBlank() || category.isBlank()) {
                _mapState.update { it.copy(addEventError = "All fields are required.") }
                return@launch
            }

            val currentUser = userRepository.getCurrentUser()
            val currentLocation = _mapState.value.lastKnownLocation

            if (currentUser == null || currentLocation == null) {
                _mapState.update { it.copy(addEventError = "User or location not available.") }
                return@launch
            }

            val newEvent = Event(
                name = name,
                description = description,
                category = category,
                location = GeoPoint(currentLocation.latitude, currentLocation.longitude),
                creatorId = currentUser.uid,
                creatorName = currentUser.username
            )

            eventRepository.addEvent(newEvent).onSuccess {
                // Ako je uspešno, samo zatvori dijalog.
                // Firestore listener će automatski osvežiti listu događaja.
                onDismissAddEventDialog()
            }.onFailure { error ->
                _mapState.update { it.copy(addEventError = error.message) }
            }
        }
    }
}
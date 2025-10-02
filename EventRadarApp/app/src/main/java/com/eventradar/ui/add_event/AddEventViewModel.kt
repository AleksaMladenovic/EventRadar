package com.eventradar.ui.add_event

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventradar.data.model.EventCategory
import com.eventradar.data.repository.EventRepository
import com.eventradar.data.repository.UserRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

sealed class AddEventResult {
    object Success : AddEventResult()
    data class Error(val message: String) : AddEventResult()
}

@HiltViewModel
class AddEventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle // Za primanje argumenata iz navigacije
) : ViewModel() {

    private val _formState = MutableStateFlow(AddEventFormState())
    val formState: StateFlow<AddEventFormState> = _formState.asStateFlow()

    private val _addEventResult = MutableSharedFlow<AddEventResult>()
    val addEventResult = _addEventResult.asSharedFlow()

    // Dobavljamo koordinate koje su prosleđene preko navigacije
    val latitude: Double = savedStateHandle.get<String>("lat")?.toDoubleOrNull() ?: 0.0
    val longitude: Double = savedStateHandle.get<String>("lng")?.toDoubleOrNull() ?: 0.0

    // --- Funkcije za ažuriranje stanja forme ---
    fun onNameChange(name: String) {
        _formState.update { it.copy(name = name, nameError = null) }
    }

    fun onDescriptionChange(description: String) {
        _formState.update { it.copy(description = description, descriptionError = null) }
    }

    fun onCategoryChange(category: EventCategory) {
        _formState.update { it.copy(category = category) }
    }


    fun onSaveEvent() {
        viewModelScope.launch {
            // TODO: Implementirati validaciju i logiku za čuvanje

            // Primer kako će izgledati čuvanje
            _formState.update { it.copy(isLoading = true) }

            val state = _formState.value
            val currentUser = userRepository.getCurrentUser()

            if (currentUser == null) {
                _addEventResult.emit(AddEventResult.Error("User not found."))
                _formState.update { it.copy(isLoading = false) }
                return@launch
            }

            // Kreiraj Event objekat...

            // Pozovi repository...

            println("Saving event at location: $latitude, $longitude")
        }
    }
}
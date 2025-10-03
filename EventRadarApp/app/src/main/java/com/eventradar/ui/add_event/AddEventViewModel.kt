package com.eventradar.ui.add_event

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventradar.data.model.Event
import com.eventradar.data.model.EventCategory
import com.eventradar.data.repository.EventRepository
import com.eventradar.data.repository.UserRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import com.eventradar.R

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

    fun onDateChanged(date: Date) {
        _formState.update { it.copy(eventDate = date, dateError = null) }
    }

    fun onTimeChanged(hour: Int, minute: Int) {
        // Formatiramo vreme u "HH:mm" format, npr. "09:30"
        val timeString = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        _formState.update { it.copy(eventTime = timeString, timeError = null) }
    }

    fun onAgeRestrictionChanged(isRestricted: Boolean) {
        _formState.update { it.copy(ageRestriction = isRestricted) }
    }

    fun onIsFreeChanged(isFree: Boolean) {
        // Ako je događaj besplatan, obriši cenu i grešku za cenu
        if (isFree) {
            _formState.update { it.copy(isFree = true, price = "", priceError = null) }
        } else {
            _formState.update { it.copy(isFree = false) }
        }
    }

    fun onPriceChanged(price: String) {
        // Dozvoli unos samo brojeva i tačke/zareza
        if (price.isEmpty() || price.matches(Regex("^\\d*\\.?\\d*\$"))) {
            _formState.update { it.copy(price = price, priceError = null) }
        }
    }
    fun onSaveEvent() {
        viewModelScope.launch {
            if (validateForm()) {
                _formState.update { it.copy(isLoading = true) }

                val state = _formState.value
                val currentUser = userRepository.getCurrentUser()

                if (currentUser == null) {
                    _addEventResult.emit(AddEventResult.Error("User not found. Please log in again."))
                    _formState.update { it.copy(isLoading = false) }
                    return@launch
                }

                // Spajanje datuma i vremena u jedan Timestamp
                val eventTimestamp = combineDateAndTime(state.eventDate, state.eventTime)

                val newEvent = Event(
                    // id se ne postavlja, Firestore će ga generisati
                    name = state.name.trim(),
                    description = state.description.trim(),
                    category = state.category.name,
                    location = GeoPoint(latitude, longitude),
                    creatorId = currentUser.uid,
                    creatorName = "${currentUser.firstName} ${currentUser.lastName}",

                    eventTimestamp = eventTimestamp,
                    ageRestriction = if (state.ageRestriction) 18 else 0,
                    isFree = state.isFree,
                    price = if (state.isFree) 0.0 else state.price.toDoubleOrNull() ?: 0.0
                    // eventImageUrl ćemo dodati kasnije
                )

                val result = eventRepository.addEvent(newEvent)
                _formState.update { it.copy(isLoading = false) }

                result.onSuccess {
                    _addEventResult.emit(AddEventResult.Success)
                }.onFailure { exception ->
                    _addEventResult.emit(AddEventResult.Error(exception.message ?: "An error occurred."))
                }
            }
        }
    }

    private fun validateForm(): Boolean {
        val state = _formState.value

        val nameError = if (state.name.isBlank()) R.string.error_field_required else null
        val descriptionError = if (state.description.isBlank()) R.string.error_field_required else null
        val dateError = if (state.eventDate == null) R.string.error_field_required else null
        val timeError = if (state.eventTime.isBlank()) R.string.error_field_required else null
        val priceError = if (!state.isFree && (state.price.isBlank() || state.price.toDoubleOrNull() == null)) {
            R.string.error_invalid_price
        } else null

        _formState.update {
            it.copy(
                nameError = nameError,
                descriptionError = descriptionError,
                dateError = dateError,
                timeError = timeError,
                priceError = priceError
            )
        }

        return listOfNotNull(nameError, descriptionError, dateError, timeError, priceError).isEmpty()
    }

    // Pomoćna funkcija za spajanje datuma i vremena
    private fun combineDateAndTime(date: Date?, time: String): Timestamp? {
        if (date == null || time.isBlank()) return null

        return try {
            val (hour, minute) = time.split(":").map { it.toInt() }
            val calendar = Calendar.getInstance().apply {
                timeInMillis = date.time
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            Timestamp(calendar.time)
        } catch (e: Exception) {
            null
        }
    }


}
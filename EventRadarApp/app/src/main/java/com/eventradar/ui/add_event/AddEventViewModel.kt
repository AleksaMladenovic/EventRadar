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
import java.text.SimpleDateFormat

sealed class AddEventResult {
    object Success : AddEventResult()
    data class Error(val message: String) : AddEventResult()
}

@HiltViewModel
class AddEventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _formState = MutableStateFlow(AddEventFormState())
    val formState: StateFlow<AddEventFormState> = _formState.asStateFlow()

    private val _addEventResult = MutableSharedFlow<AddEventResult>()
    val addEventResult = _addEventResult.asSharedFlow()

    // Proveravamo da li smo u Edit modu
    private val eventId: String? = savedStateHandle.get<String>("eventId")
    private val isEditMode = eventId != null

    // Koordinate za Create mod
    private val latForCreate: Double = savedStateHandle.get<String>("lat")?.toDoubleOrNull() ?: 0.0
    private val lngForCreate: Double = savedStateHandle.get<String>("lng")?.toDoubleOrNull() ?: 0.0

    init {
        if (isEditMode) {
            loadEventForEditing(eventId!!)
        }
    }


    private fun loadEventForEditing(id: String) {
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            eventRepository.getEventById(id).first().onSuccess { event -> // .first() da uzmemo samo jednu vrednost
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                _formState.update {
                    it.copy(
                        isLoading = false,
                        name = event.name,
                        description = event.description,
                        category = EventCategory.fromString(event.category),
                        eventDate = event.eventTimestamp?.toDate(),
                        eventTime = event.eventTimestamp?.toDate()?.let { d -> timeFormat.format(d) } ?: "",
                        ageRestriction = event.ageRestriction > 0,
                        free = event.free,
                        price = if (event.free) "" else event.price.toString()
                    )
                }
            }.onFailure {

            }
        }
    }
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

    fun onIsFreeChanged(free: Boolean) {
        // Ako je događaj besplatan, obriši cenu i grešku za cenu
        if (free) {
            _formState.update { it.copy(free= true, price = "", priceError = null) }
        } else {
            _formState.update { it.copy(free = false) }
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

                if (isEditMode) {

                    // 1. Dobavi originalni događaj iz baze
                    val originalEventResult = eventRepository.getEventById(eventId!!).first()
                    if (originalEventResult.isFailure) {
                        _addEventResult.emit(AddEventResult.Error("Failed to fetch original event for update."))
                        _formState.update { it.copy(isLoading = false) }
                        return@launch
                    }
                    val originalEvent = originalEventResult.getOrNull()!!

                    // 2. Kreiraj NOVI objekat tako što kopiraš STARI i prepišeš samo izmenjena polja
                    val updatedEvent = originalEvent.copy(
                        name = _formState.value.name.trim(),
                        description = _formState.value.description.trim(),
                        category = _formState.value.category.name,
                        eventTimestamp = combineDateAndTime(_formState.value.eventDate, _formState.value.eventTime),
                        ageRestriction = if (_formState.value.ageRestriction) 18 else 0,
                        free = _formState.value.free,
                        price = if (_formState.value.free) 0.0 else _formState.value.price.toDoubleOrNull() ?: 0.0
                        // Sva ostala polja (id, location, creatorId, itd.) ostaju ista kao u 'originalEvent'
                    )

                    // 3. Pošalji kompletan, ažuriran objekat na update
                    val result = eventRepository.updateEvent(updatedEvent)

                    _formState.update { it.copy(isLoading = false) }
                    if (result.isSuccess) _addEventResult.emit(AddEventResult.Success)
                    else _addEventResult.emit(AddEventResult.Error(result.exceptionOrNull()?.message ?: "Update failed."))

                } else {
                    // --- LOGIKA ZA CREATE (ostaje ista) ---
                    val currentUser = userRepository.getCurrentUser()
                    if (currentUser == null) {
                        _addEventResult.emit(AddEventResult.Error("User not found."))
                        _formState.update { it.copy(isLoading = false) }
                        return@launch
                    }

                    val newEvent = createEventFromState().copy(
                        creatorId = currentUser.uid,
                        creatorName = "${currentUser.firstName} ${currentUser.lastName}",
                        location = GeoPoint(latForCreate, lngForCreate)
                    )

                    val result = eventRepository.addEvent(newEvent)
                    _formState.update { it.copy(isLoading = false) }

                    if (result.isSuccess) _addEventResult.emit(AddEventResult.Success)
                    else _addEventResult.emit(AddEventResult.Error(result.exceptionOrNull()?.message ?: "Create failed."))
                }
            }
        }
    }


    // Pomoćna funkcija da se izbegne dupliranje koda
    private fun createEventFromState(): Event {
        val state = _formState.value
        val eventTimestamp = combineDateAndTime(state.eventDate, state.eventTime)
        return Event(
            name = state.name.trim(),
            description = state.description.trim(),
            category = state.category.name,
            eventTimestamp = eventTimestamp,
            ageRestriction = if (state.ageRestriction) 18 else 0,
            free = state.free,
            price = if (state.free) 0.0 else state.price.toDoubleOrNull() ?: 0.0
        )
    }

    private fun validateForm(): Boolean {
        val state = _formState.value

        val nameError = if (state.name.isBlank()) R.string.error_field_required else null
        val descriptionError = if (state.description.isBlank()) R.string.error_field_required else null
        val dateError = if (state.eventDate == null) R.string.error_field_required else null
        val timeError = if (state.eventTime.isBlank()) R.string.error_field_required else null
        val priceError = if (!state.free && (state.price.isBlank() || state.price.toDoubleOrNull() == null)) {
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
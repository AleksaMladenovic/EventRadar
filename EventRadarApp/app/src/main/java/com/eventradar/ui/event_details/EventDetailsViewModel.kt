package com.eventradar.ui.event_details

import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventradar.data.model.Comment
import com.eventradar.data.model.CommentWithAuthor
import com.eventradar.data.repository.AuthRepository
import com.eventradar.data.repository.CommentRepository
import com.eventradar.data.repository.EventRepository
import com.eventradar.data.repository.UserRepository
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventDetailsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val commentRepository: CommentRepository,
    private val ioDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(EventDetailsState())
    val state: StateFlow<EventDetailsState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<EventDetailsEvent>()
    val event = _event.asSharedFlow()

    private val currentUserId: String? = authRepository.getCurrentUserId()
    val eventId: String? = savedStateHandle.get("eventId")

    init {
        // Čitamo 'eventId' iz argumenata rute
        if (eventId != null) {
            getEventDetails(eventId)
            getComments(eventId)
        } else {
            // Slučaj ako ID nije prosleđen - prikaži grešku
            _state.update { it.copy(isLoading = false, error = "Event ID not provided.") }
        }
    }

    private fun getEventDetails(eventId: String) {
        // .onEach se poziva svaki put kada Flow emituje novu vrednost
        eventRepository.getEventById(eventId)
            .flowOn(ioDispatcher)
            .onEach { result ->
                result.onSuccess { event ->
                    val isAttending = currentUserId?.let {it in event.attendeeIds}?:false

                    val averageRating = if(event.ratingCount>0){
                        (event.ratingSum/event.ratingCount).toFloat()
                    }else{
                        0f
                    }
                    val currentUserRating = event.ratedByUserIds[currentUserId]?.toInt() ?: 0

                    // Ako je uspešno, ažuriraj stanje sa podacima o događaju
                    _state.update {
                        it.copy(
                            event = event,
                            isLoading = false,
                            error = null,
                            isCurrentUserAttending = isAttending,
                            averageRating = averageRating,
                            currentUserRating = currentUserRating,
                        ) }
                }.onFailure { exception ->
                    // Ako je neuspešno, ažuriraj stanje sa porukom o grešci
                    _state.update { it.copy(isLoading = false, error = exception.message) }
                }
            }
            .launchIn(viewModelScope) // Pokreni sakupljanje (collecting) u ViewModelScope
    }
    fun isCurrentUserOwner(creatorId: String): Boolean {
        return authRepository.getCurrentUserId() == creatorId
    }

    fun onDeleteEvent() {
        viewModelScope.launch(ioDispatcher) {
            val eventId = state.value.event?.id
            if (eventId == null) {
                _event.emit(EventDetailsEvent.DeletionError("Event ID is missing."))
                return@launch
            }

            val result = eventRepository.deleteEvent(eventId)

            if (result.isSuccess) {
                _event.emit(EventDetailsEvent.DeletionSuccess)
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "An unknown error occurred."
                _event.emit(EventDetailsEvent.DeletionError(errorMessage))
            }
        }
    }

    fun onToggleAttendanceClick() {
        viewModelScope.launch(ioDispatcher) {
            val eventId = state.value.event?.id
            if (eventId == null || currentUserId == null) return@launch

            val isCurrentlyAttending = state.value.isCurrentUserAttending
            _state.update { it.copy(isCurrentUserAttending = !isCurrentlyAttending) }

            val result = eventRepository.toggleAttendance(eventId, currentUserId)

            result.onFailure {
                _state.update { it.copy(isCurrentUserAttending = isCurrentlyAttending) }
                // TODO: Prikazati Toast poruku o grešci
            }
        }
    }

    private fun getComments(eventId: String) {
        viewModelScope.launch(ioDispatcher) {
            commentRepository.getCommentsForEvent(eventId).collect { result ->
                result.onSuccess { comments ->
                    // Dobili smo listu "sirovih" komentara
                    val commentsWithAuthors = comments.map { comment ->
                        // Za svaki komentar, JEDNOKRATNO povuci podatke o autoru
                        val authorResult = userRepository.getUserById(comment.authorId)
                        CommentWithAuthor(
                            comment = comment,
                            author = authorResult.getOrNull()
                        )
                    }
                    _state.update { it.copy(comments = commentsWithAuthors) }
                }
            }
        }
    }

    fun onAddComment(text: String) {
        viewModelScope.launch(ioDispatcher) {
            if (eventId != null) {
                commentRepository.addComment(eventId, text).onFailure {
                    // TODO: Prikazati grešku korisniku
                    println("Failed to add comment: ${it.message}")
                }
            }
        }
    }

    fun onRatingChanged(newRating: Int) {
        viewModelScope.launch(ioDispatcher) {
            val eventId = state.value.event?.id ?: return@launch

            _state.update { it.copy(currentUserRating = newRating) }

            val result = eventRepository.rateEvent(eventId, newRating.toDouble())

            result.onFailure {
                println("Failed to submit rating: ${it.message}")
            }
        }
    }

}

sealed class EventDetailsEvent {
    object DeletionSuccess : EventDetailsEvent()
    data class DeletionError(val message: String) : EventDetailsEvent()
}
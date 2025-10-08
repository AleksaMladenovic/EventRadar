package com.eventradar.ui.event_details

import com.eventradar.data.model.CommentWithAuthor
import com.eventradar.data.model.Event

data class EventDetailsState(
    val event: Event? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isCurrentUserAttending: Boolean = false,
    val comments: List<CommentWithAuthor> = emptyList(),
    val averageRating: Float = 0f,
    val currentUserRating: Int = 0,
)
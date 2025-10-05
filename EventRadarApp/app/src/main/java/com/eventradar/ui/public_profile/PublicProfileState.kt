package com.eventradar.ui.public_profile

import com.eventradar.data.model.Event
import com.eventradar.data.model.User

data class PublicProfileState(
    val user: User? = null,
    val userEvents: List<Event> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
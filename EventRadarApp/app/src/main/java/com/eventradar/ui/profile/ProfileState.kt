package com.eventradar.ui.profile

import com.eventradar.data.model.Event
import com.eventradar.data.model.User

data class ProfileState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val myEvents: List<Event> = emptyList(),
)
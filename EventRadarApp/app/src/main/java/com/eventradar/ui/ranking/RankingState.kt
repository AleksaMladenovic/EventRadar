package com.eventradar.ui.ranking

import com.eventradar.data.model.User

data class RankingState(
    val topThreeUsers: List<User> = emptyList(),
    val otherUsers: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
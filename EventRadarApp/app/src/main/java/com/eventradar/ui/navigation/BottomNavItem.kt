package com.eventradar.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.eventradar.navigation.Routes
import com.eventradar.R

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleResId: Int,
    val icon: ImageVector
) {
    object Map : BottomNavItem(
        route = Routes.MAP_SCREEN,
        titleResId = R.string.bottom_nav_map,
        icon = Icons.Default.Map
    )

    object EventsList : BottomNavItem(
        route = Routes.EVENTS_LIST_SCREEN,
        titleResId = R.string.bottom_nav_events,
        icon = Icons.AutoMirrored.Filled.FormatListBulleted
    )

    object Ranking : BottomNavItem(
        route = Routes.RANKING_SCREEN,
        titleResId = R.string.bottom_nav_ranking,
        icon = Icons.Default.EmojiEvents
    )

    object Profile : BottomNavItem(
        route = Routes.PROFILE_SCREEN,
        titleResId = R.string.bottom_nav_profile,
        icon = Icons.Default.Person
    )
}
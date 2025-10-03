package com.eventradar.navigation.graphs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.eventradar.navigation.Routes
import com.eventradar.ui.events_list.EventsListScreen
import com.eventradar.ui.map.MapScreen
import com.eventradar.ui.profile.ProfileScreen
import com.eventradar.ui.ranking.RankingScreen

@Composable
fun HomeNavGraph(navController: NavHostController, rootNavController: NavController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Routes.MAP_SCREEN,
        modifier = modifier
    ) {
        composable(Routes.MAP_SCREEN) {
            MapScreen(
                onNavigateToAddEvent = { latLng ->
                    rootNavController.navigate("add_event/${latLng.latitude}/${latLng.longitude}")
                },
                onNavigateToEventDetails = { eventId ->
                    rootNavController.navigate("event_details/$eventId")
                }

            )
        }
        composable(Routes.EVENTS_LIST_SCREEN) {
            EventsListScreen(
                onNavigateToEventDetails = { eventId ->
                    rootNavController.navigate("event_details/$eventId")
                })
        }
        composable(Routes.RANKING_SCREEN) {
            RankingScreen()
        }
        composable(Routes.PROFILE_SCREEN) {
            ProfileScreen()
        }
    }
}
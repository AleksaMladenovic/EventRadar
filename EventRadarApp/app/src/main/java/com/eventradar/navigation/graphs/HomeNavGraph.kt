package com.eventradar.navigation.graphs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.eventradar.navigation.Routes
import com.eventradar.ui.add_event.AddEventScreen
import com.eventradar.ui.event_details.EventDetailsScreen
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
        composable(
            route= Routes.MAP_SCREEN,
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType; nullable = true },
                navArgument("lng") { type = NavType.StringType; nullable = true }
            )
        ) {backStackEntry ->
            MapScreen(
                arguments = backStackEntry.arguments,
                onNavigateToAddEvent = { latLng ->
                    navController.navigate("add_event/${latLng.latitude}/${latLng.longitude}")
                },
                onNavigateToEventDetails = { eventId ->
                    navController.navigate("event_details/$eventId")
                }
            )

        }
        composable(Routes.EVENTS_LIST_SCREEN) {
            EventsListScreen(
                onNavigateToEventDetails = { eventId ->
                    navController.navigate("event_details/$eventId")
                })
        }
        composable(Routes.RANKING_SCREEN) {
            RankingScreen()
        }
        composable(Routes.PROFILE_SCREEN) {
            ProfileScreen()
        }
        composable(
            route = Routes.ADD_EVENT_SCREEN,
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType },
                navArgument("lng") { type = NavType.StringType }
            )
        ) {
            AddEventScreen(
                onEventAdded = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = Routes.EVENT_DETAILS_SCREEN,
            arguments = listOf(
                navArgument("eventId") {
                    type = NavType.StringType
                }
            )
        ) {
            EventDetailsScreen(
                onNavigateToMap = { latLng ->
                    // Sada samo pozivamo navigaciju na mapu sa argumentima
                    navController.navigate("map?lat=${latLng.latitude}&lng=${latLng.longitude}") {
                        // Vraća nas na mapu i osigurava da imamo samo jednu instancu mape
                        popUpTo(Routes.MAP_SCREEN) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
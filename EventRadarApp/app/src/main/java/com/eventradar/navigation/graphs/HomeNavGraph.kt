package com.eventradar.navigation.graphs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.eventradar.navigation.Routes
import com.eventradar.ui.add_event.AddEventScreen
import com.eventradar.ui.auth.AuthViewModel
import com.eventradar.ui.event_details.EventDetailsScreen
import com.eventradar.ui.events_list.EventsListScreen
import com.eventradar.ui.map.MapScreen
import com.eventradar.ui.profile.ProfileScreen
import com.eventradar.ui.public_profile.PublicProfileScreen
import com.eventradar.ui.ranking.RankingScreen

@Composable
fun HomeNavGraph(homeNavController: NavHostController, rootNavController: NavController, modifier: Modifier = Modifier) {
    NavHost(
        navController = homeNavController,
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
                    homeNavController.navigate("add_event/${latLng.latitude}/${latLng.longitude}")
                },
                onNavigateToEventDetails = { eventId ->
                    homeNavController.navigate("event_details/$eventId")
                }
            )

        }
        composable(
            route= Routes.EVENTS_LIST_SCREEN
            ) {
            EventsListScreen(
                onNavigateToEventDetails = { eventId ->
                    homeNavController.navigate("event_details/$eventId")
                },
            )
        }

        composable(
            route = Routes.USER_EVENTS_SCREEN,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) {
            EventsListScreen(
                onNavigateToEventDetails = { eventId ->
                    homeNavController.navigate("event_details/$eventId")
                }
            )
        }
        composable(Routes.RANKING_SCREEN) {
            RankingScreen()
        }
        composable(Routes.PROFILE_SCREEN) {
            ProfileScreen(
                onNavigateToMyEvents = { currentUserId->
                    homeNavController.navigate("user_events/$currentUserId")
                }
            )
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
                    homeNavController.popBackStack()
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
                    homeNavController.navigate("map?lat=${latLng.latitude}&lng=${latLng.longitude}") {
                        // Vraća nas na mapu i osigurava da imamo samo jednu instancu mape
                        popUpTo(Routes.MAP_SCREEN) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    homeNavController.popBackStack()
                },
                onCreatorClick = { userId ->
                    homeNavController.navigate("public_profile/$userId")
                }
            )
        }

        composable(
            route = Routes.PUBLIC_PROFILE_SCREEN,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) {
            PublicProfileScreen(
                onNavigateBack = { homeNavController.popBackStack() },
                onNavigateToEventDetails = { eventId ->
                    // Sa ovog ekrana takođe možemo da idemo na detalje događaja
                    homeNavController.navigate("event_details/$eventId")
                }
            )
        }
    }
}
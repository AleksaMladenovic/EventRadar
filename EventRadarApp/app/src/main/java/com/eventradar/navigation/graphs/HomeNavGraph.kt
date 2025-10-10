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
import com.eventradar.ui.change_password.ChangePasswordScreen
import com.eventradar.ui.edit_profile.EditProfileScreen
import com.eventradar.ui.event_details.EventDetailsScreen
import com.eventradar.ui.events_list.EventsListScreen
import com.eventradar.ui.location_picker.LocationPickerScreen
import com.eventradar.ui.map.MapScreen
import com.eventradar.ui.profile.ProfileScreen
import com.eventradar.ui.public_profile.PublicProfileScreen
import com.eventradar.ui.ranking.RankingScreen
import com.google.android.gms.maps.model.LatLng

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
                    homeNavController.navigate("add_event?lat=${latLng.latitude}&lng=${latLng.longitude}")
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
                navArgument("createdByUserId") { type = NavType.StringType }
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
                },
                onNavigateToAttendingEvents = {currentUserId ->
                    homeNavController.navigate("attending_events/${currentUserId}")
                },
                onNavigateToEditProfile = {
                    homeNavController.navigate(Routes.EDIT_PROFILE_SCREEN)
                },
                onNavigateToChangePassword = {
                    homeNavController.navigate(Routes.CHANGE_PASSWORD_SCREEN)
                }
            )
        }
        composable(
            route = Routes.ADD_EVENT_SCREEN,
            arguments = listOf(
                navArgument("eventId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("lat") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("lng") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) {
            AddEventScreen(
                onEventAdded = {
                    homeNavController.popBackStack()
                },
                navController = homeNavController,
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
                    homeNavController.navigate("map?lat=${latLng.latitude}&lng=${latLng.longitude}") {
                        popUpTo(Routes.MAP_SCREEN) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    homeNavController.popBackStack()
                },
                onCreatorClick = { userId ->
                    homeNavController.navigate("public_profile/$userId")
                },
                onNavigateToEditEvent = { eventId ->
                    homeNavController.navigate("add_event?eventId=$eventId")
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

        composable(
            route = Routes.LOCATION_PICKER_SCREEN,
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType },
                navArgument("lng") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
            val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 0.0
            val initialLocation = LatLng(lat, lng)

            LocationPickerScreen(
                initialLocation = initialLocation,
                onLocationSelected = { newLocation ->
                    homeNavController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("picked_location", newLocation)

                    homeNavController.popBackStack()
                },
                onNavigateBack = {
                    homeNavController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.ATTENDING_EVENTS_SCREEN,
            arguments = listOf(
                navArgument("attendingUserId") { type = NavType.StringType }
            )
        ) {
            EventsListScreen(
                onNavigateToEventDetails = { eventId ->
                    homeNavController.navigate("event_details/$eventId")
                }
            )
        }

        composable(Routes.EDIT_PROFILE_SCREEN) {
            EditProfileScreen(
                onNavigateBack = {
                    homeNavController.popBackStack()
                }
            )
        }

        composable(Routes.CHANGE_PASSWORD_SCREEN) {
            ChangePasswordScreen(
                onNavigateBack = { homeNavController.popBackStack() }
            )
        }

    }
}
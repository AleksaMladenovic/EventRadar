package com.eventradar.navigation.graphs


import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.eventradar.navigation.Routes
import com.eventradar.ui.add_event.AddEventScreen
import com.eventradar.ui.auth.AuthViewModel
import com.eventradar.ui.event_details.EventDetailsScreen
import com.eventradar.ui.main.MainScreen
import com.eventradar.ui.map.MapScreen

fun NavGraphBuilder.mainNavGraph(navController: NavController) {
    navigation(
        startDestination = "main_flow",
        route = Routes.MAIN_GRAPH
    ) {
        composable("main_flow") {
            MainScreen(rootNavController = navController)
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
            EventDetailsScreen()
        }

    }
}
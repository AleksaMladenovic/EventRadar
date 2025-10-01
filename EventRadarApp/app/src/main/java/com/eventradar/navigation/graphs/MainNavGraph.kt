package com.eventradar.navigation.graphs


import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.eventradar.navigation.Routes
import com.eventradar.ui.auth.AuthViewModel
import com.eventradar.ui.main.MainScreen
import com.eventradar.ui.map.MapScreen

fun NavGraphBuilder.mainNavGraph(navController: NavController) {
    navigation(
        startDestination = "main_flow",
        route = Routes.MAIN_GRAPH
    ) {
        composable("main_flow") {
            MainScreen()
        }
    }
}
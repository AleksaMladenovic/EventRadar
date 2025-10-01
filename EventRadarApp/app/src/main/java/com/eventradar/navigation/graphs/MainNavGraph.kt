package com.eventradar.navigation.graphs


import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.eventradar.navigation.Routes
import com.eventradar.ui.auth.AuthViewModel
import com.eventradar.ui.map.MapScreen

fun NavGraphBuilder.mainNavGraph(navController: NavController) {
    navigation(
        startDestination = Routes.MAP_SCREEN,
        route = Routes.MAIN_GRAPH
    ) {
        composable(Routes.MAP_SCREEN) {
            val authViewModel: AuthViewModel = hiltViewModel() // ViewModel za odjavu
            MapScreen(
                onLogout = {
                    authViewModel.signOut() // AuthViewModel će promeniti stanje
                    // Ne moramo eksplicitno da navigiramo, RootNavGraph će to uraditi!
                }
            )
        }
        // Ovde ćeš kasnije dodati composable(Routes.PROFILE_SCREEN) { ... }
    }
}
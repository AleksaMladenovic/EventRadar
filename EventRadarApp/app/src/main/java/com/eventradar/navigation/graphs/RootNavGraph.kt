package com.eventradar.navigation.graphs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.eventradar.navigation.Routes
import com.eventradar.ui.auth.AuthState
import com.eventradar.ui.auth.AuthViewModel
import com.eventradar.ui.welcome.WelcomeScreen

@Composable
fun RootNavGraph() {
    val navController = rememberNavController()
    // Kreiramo AuthViewModel na najvišem nivou, jer nam treba za proveru stanja
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            navController.navigate(Routes.MAIN_GRAPH) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        route = Routes.ROOT_GRAPH,
        // Dinamički određujemo početnu tačku na osnovu stanja prijave
        startDestination = if (authState is AuthState.Authenticated) Routes.MAIN_GRAPH else Routes.AUTH_GRAPH
    ) {

        // Uključujemo naše pod-grafove
        authNavGraph(navController = navController)
        mainNavGraph(navController = navController)
    }
}
package com.eventradar.navigation.graphs

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.eventradar.navigation.Routes
import com.eventradar.ui.auth.login.LoginViewModel
import com.eventradar.ui.auth.register.RegisterViewModel
import com.eventradar.ui.auth.login.LoginScreen
import com.eventradar.ui.auth.register.RegisterScreen

fun NavGraphBuilder.authNavGraph(navController: NavController) {
    navigation(
        startDestination = Routes.LOGIN_SCREEN,
        route = Routes.AUTH_GRAPH
    ) {
        composable(Routes.LOGIN_SCREEN) {
            val loginViewModel: LoginViewModel = hiltViewModel() // Kreiramo ViewModel ovde
            LoginScreen(
                loginViewModel = loginViewModel,
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER_SCREEN)
                },
                onLoginSuccess = {
                    // Nakon uspeha, idi na glavni graf i obriši ceo auth graf iz istorije
                    navController.navigate(Routes.MAIN_GRAPH) {
                        popUpTo(Routes.AUTH_GRAPH) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable(Routes.REGISTER_SCREEN) {
            val registerViewModel: RegisterViewModel = hiltViewModel()
            RegisterScreen(
                registerViewModel = registerViewModel,
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN_SCREEN)
                },
                onRegisterSuccess = {
                    navController.navigate(Routes.MAIN_GRAPH) {
                        popUpTo(Routes.AUTH_GRAPH) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}
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
import com.eventradar.ui.welcome.WelcomeScreen

fun NavGraphBuilder.authNavGraph(navController: NavController) {
    navigation(
        startDestination = Routes.WELCOME_SCREEN,
        route = Routes.AUTH_GRAPH
    ) {
        composable(Routes.WELCOME_SCREEN) {
            WelcomeScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN_SCREEN)
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER_SCREEN)
                }
            )
        }

        composable(Routes.LOGIN_SCREEN) {
            val loginViewModel: LoginViewModel = hiltViewModel() // Kreiramo ViewModel ovde
            LoginScreen(
                loginViewModel = loginViewModel,
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER_SCREEN){
                        popUpTo(Routes.LOGIN_SCREEN){inclusive = true}
                    }
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
                    navController.navigate(Routes.LOGIN_SCREEN){
                        popUpTo(Routes.REGISTER_SCREEN) {inclusive = true}
                    }
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
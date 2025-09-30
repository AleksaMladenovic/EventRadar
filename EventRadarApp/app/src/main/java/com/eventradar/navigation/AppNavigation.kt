package com.eventradar.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.eventradar.ui.auth.AuthState
import com.eventradar.ui.auth.AuthViewModel
import com.eventradar.ui.auth.LoginScreen
import com.eventradar.ui.auth.RegisterScreen
import com.eventradar.ui.map.MapScreen

@Composable
fun AppNavigation(authViewModel: AuthViewModel){
    val navController = rememberNavController();
    val authState = authViewModel.authState.collectAsStateWithLifecycle()

    LaunchedEffect(authState.value) {
        when(authState.value){
            is AuthState.Authenticated -> navController.navigate(Screen.MapScreen.route){
                popUpTo(Screen.MapScreen.route){
                    inclusive = true
                }
            }
            is AuthState.Unauthenticated -> navController.navigate(Screen.LoginScreen.route){
                popUpTo(Screen.LoginScreen.route){
                    inclusive = true
                }
            }
            else -> Unit
        }
    }

    NavHost(navController = navController, startDestination = Screen.LoginScreen.route ){
        composable (Screen.LoginScreen.route){
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.RegisterScreen.route)
                })
        }
        composable (Screen.RegisterScreen.route){
            RegisterScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate(Screen.LoginScreen.route)
                })
        }
        composable(Screen.MapScreen.route){
            MapScreen( authViewModel = authViewModel)
        }
    }


}
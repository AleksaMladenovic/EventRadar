package com.eventradar.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.eventradar.ui.auth.LoginScreen
import com.eventradar.ui.map.MapScreen

@Composable
fun AppNavigation(){
    val navController = rememberNavController();

    NavHost(navController = navController, startDestination = Screen.LoginScreen.route ){
        composable (Screen.LoginScreen.route){
            LoginScreen(navController = navController)
        }
        composable(Screen.MapScreen.route){
            MapScreen(navController = navController)
        }
    }
}
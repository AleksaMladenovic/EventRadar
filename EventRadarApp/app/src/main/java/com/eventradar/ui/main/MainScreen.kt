package com.eventradar.ui.main

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.eventradar.navigation.graphs.HomeNavGraph
import com.eventradar.ui.components.BottomBar
import com.eventradar.ui.navigation.BottomNavItem

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen() {
    // Kreiramo NOVI NavController samo za ekrane UNUTAR MainScreen-a
    val homeNavController = rememberNavController()

    Scaffold(
        // Prosleđujemo našu BottomBar komponentu u bottomBar slot
        bottomBar = {
            BottomBar(
                navController = homeNavController,
                items = listOf(
                    BottomNavItem.Map,
                    BottomNavItem.EventsList,
                    BottomNavItem.Ranking,
                    BottomNavItem.Profile
                )
            )
        }
    ) { paddingValues ->
        // Unutar content slota Scaffold-a, postavljamo novi, ugnježdeni NavHost.
        // On će prikazivati ekrane u prostoru IZNAD donje navigacije.
        HomeNavGraph(
            navController = homeNavController,
            modifier = Modifier.padding(paddingValues) // Važno da se content ne preklapa sa bottom barom
        )
    }
}
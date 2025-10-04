package com.eventradar.ui.main

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.eventradar.navigation.graphs.HomeNavGraph
import com.eventradar.ui.components.BottomBar
import com.eventradar.ui.filters.FilterBottomSheet
import com.eventradar.ui.navigation.BottomNavItem


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(rootNavController: NavController) {
    val homeNavController = rememberNavController()
    val sheetState = rememberModalBottomSheetState()
    var isSheetOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EventRadar") },
                actions = {
                    IconButton(onClick = { isSheetOpen = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter Events")
                    }
                }
            )
        },
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
        HomeNavGraph(
            navController = homeNavController,
            rootNavController = rootNavController,
            modifier = Modifier.padding(paddingValues)
        )
    }

    if (isSheetOpen) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { isSheetOpen = false }
        ) {
            FilterBottomSheet(onApplyFilters = {
                isSheetOpen = false
            })
        }
    }
}
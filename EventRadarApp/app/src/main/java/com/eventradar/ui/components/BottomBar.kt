package com.eventradar.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.eventradar.ui.navigation.BottomNavItem

@Composable
fun BottomBar(
    navController: NavController,
    items: List<BottomNavItem>
) {
    val backStackEntry = navController.currentBackStackEntryAsState()

    NavigationBar{
        items.forEach { item ->
            val isSelected = item.route == backStackEntry.value?.destination?.route

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                label = {
                    Text(text = stringResource(id = item.titleResId))
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(id = item.titleResId)
                    )
                }
            )
        }
    }
}
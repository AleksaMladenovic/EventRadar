package com.eventradar.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.eventradar.ui.auth.AuthViewModel

@Composable
fun ProfileScreen(
) {
    val authViewModel: AuthViewModel = hiltViewModel()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Profile Screen")
            Button(onClick = {
                authViewModel.signOut()
            }) {
                Text("Sign Out")
            }
        }
    }
}
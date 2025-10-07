package com.eventradar.ui.location_picker

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    initialLocation: LatLng,
    onLocationSelected: (LatLng) -> Unit,
    onNavigateBack: () -> Unit
) {
    // Pamtimo stanje kamere, postavljeno na početnu lokaciju
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 15f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Location") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // Kada se potvrdi, vraćamo koordinate centra mape
                val selectedLocation = cameraPositionState.position.target
                onLocationSelected(selectedLocation)
            }) {
                Icon(Icons.Default.Check, contentDescription = "Confirm Location")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Mapa preko celog ekrana
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true)
            )

            // Fiksni marker u centru
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Center Marker",
                modifier = Modifier.align(Alignment.Center).size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
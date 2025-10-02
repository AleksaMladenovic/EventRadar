package com.eventradar.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventradar.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState


@Composable
fun MapScreen(
    mapViewModel: MapViewModel = hiltViewModel()
){
    val mapState by mapViewModel.mapState.collectAsStateWithLifecycle()
    var hasLocationPermission by remember { mutableStateOf(false) }
    var isInitialCameraAnimationDone by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                hasLocationPermission = true
            }
        }
    )
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val cameraPositionState = rememberCameraPositionState ()

    LaunchedEffect(mapState.lastKnownLocation) {
        if(mapState.lastKnownLocation!=null&& !isInitialCameraAnimationDone){
            val userLatLng = LatLng(mapState.lastKnownLocation!!.latitude, mapState.lastKnownLocation!!.longitude)

            cameraPositionState.animate(
                update = CameraUpdateFactory.newCameraPosition(
                    CameraPosition(userLatLng, 15f, 0f, 0f)
                ),
                durationMs = 1000
            )
            isInitialCameraAnimationDone = true
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, hasLocationPermission) {
        val observer = LifecycleEventObserver { _, event ->
            if (hasLocationPermission) {
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        mapViewModel.startLocationUpdates()
                        println("MAP LIFECYCLE: Starting location updates")
                    }
                    Lifecycle.Event.ON_STOP -> {
                        mapViewModel.stopLocationUpdates()
                        println("MAP LIFECYCLE: Stopping location updates")
                    }

                    else -> {}
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewModel.stopLocationUpdates()
            println("MAP LIFECYCLE: Disposed, stopping updates")
        }
    }
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { mapViewModel.onShowAddEventDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        }
    ) { paddingValues ->
        // Prikazujemo dijalog ako je 'isAddEventDialogShown' true
        if (mapState.isAddEventDialogShown) {
            AddEventDialog(
                onDismiss = { mapViewModel.onDismissAddEventDialog() },
                onConfirm = { name, description, category ->
                    mapViewModel.addEvent(name, description, category)
                },
                errorMessage = mapState.addEventError
            )
        }
        if(hasLocationPermission){
            Box(
                modifier = Modifier.padding(paddingValues)
            ){
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = true,
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = true,
                    )
                ){
                    mapState.events.forEach { event ->
                        Marker(
                            state = MarkerState(
                                position = LatLng(event.location.latitude, event.location.longitude),
                            ),
                            title = event.name,
                            snippet = event.description,
                        )
                    }
                }
            }

        }else{
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ){
                Text(text = stringResource(R.string.location_permission_required))
            }

        }
    }

}
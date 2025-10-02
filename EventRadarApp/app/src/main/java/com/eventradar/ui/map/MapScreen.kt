package com.eventradar.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventradar.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

@Composable
fun MapScreen(
    mapViewModel: MapViewModel = hiltViewModel(),
    onNavigateToAddEvent: (LatLng) -> Unit,
    onNavigateToEventDetails: (String) -> Unit
) {
    // --- STANJA I LOGIKA ---
    val mapState by mapViewModel.mapState.collectAsStateWithLifecycle()
    var hasLocationPermission by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Launcher za traženje dozvola
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        }
    )

    // Jednokratno traženje dozvola pri pokretanju ekrana
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Upravljanje start/stop praćenjem lokacije
    DisposableEffect(lifecycleOwner, hasLocationPermission) {
        val observer = LifecycleEventObserver { _, event ->
            if (hasLocationPermission) {
                when (event) {
                    Lifecycle.Event.ON_START -> mapViewModel.startLocationUpdates()
                    Lifecycle.Event.ON_STOP -> mapViewModel.stopLocationUpdates()
                    else -> {}
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if(!mapState.isInitialCameraAnimationDone){
            snapshotFlow { mapState.lastKnownLocation }
                .filterNotNull()
                .first()
                .let { location ->
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newCameraPosition(
                            CameraPosition(userLatLng, 15f, 0f, 0f)
                        ),
                        durationMs = 1000
                    )
                    mapViewModel.onInitialCameraAnimationDone()
                }

        }
    }


    // --- UI DEO ---
    Box(modifier = Modifier.fillMaxSize()) {
        if (hasLocationPermission) {
            // Glavna mapa
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false)
            ) {
                // Prikaz postojećih markera za događaje
                mapState.events.forEach { event ->
                    Marker(
                        state = MarkerState(position = LatLng(event.location.latitude, event.location.longitude)),
                        title = event.name,
                        snippet = event.description,
                        onInfoWindowClick = {
                            onNavigateToEventDetails(event.id)
                        }
                    )
                }
            }

            // Prikazuje se samo ako smo u modu za dodavanje
            if (mapState.isInAddEventMode) {
                // Centralni marker koji se ne pomera
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = stringResource(R.string.cd_center_marker),
                    modifier = Modifier.align(Alignment.Center).size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                // Kontrole na dnu za potvrdu ili otkazivanje
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(
                        onClick = { mapViewModel.onExitAddEventMode() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel_button))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.cancel_button))
                    }
                    Button(onClick = {
                        val centerOfMap = cameraPositionState.position.target
                        onNavigateToAddEvent(centerOfMap)
                        // Odmah izađi iz moda dodavanja nakon potvrde
                        mapViewModel.onExitAddEventMode()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.confirm_button))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.confirm_button))
                    }
                }
            }

            // Glavni FAB '+' se prikazuje samo ako NISMO u modu za dodavanje
            AnimatedVisibility(
                visible = !mapState.isInAddEventMode,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                FloatingActionButton(onClick = { mapViewModel.onEnterAddEventMode() }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_event))
                }
            }
        } else {
            // Prikaz ako nema dozvole
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(R.string.location_permission_required))
            }
        }
    }
}
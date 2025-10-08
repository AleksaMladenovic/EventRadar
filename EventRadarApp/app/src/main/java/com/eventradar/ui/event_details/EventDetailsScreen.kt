package com.eventradar.ui.event_details

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.eventradar.R
import com.eventradar.data.model.Event
import com.eventradar.data.model.EventCategory
import com.eventradar.ui.components.CategoryChip
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    viewModel: EventDetailsViewModel = hiltViewModel(),
    onNavigateToMap: (LatLng) -> Unit,
    onNavigateBack: () -> Unit,
    onCreatorClick: (String) -> Unit,
    onNavigateToEditEvent: (String) -> Unit,
    ) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isOwner = state.event?.let { viewModel.isCurrentUserOwner(it.creatorId) } ?: false
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is EventDetailsEvent.DeletionSuccess -> {
                    Toast.makeText(context, "Event deleted successfully", Toast.LENGTH_SHORT).show()
                    onNavigateBack() // Vrati korisnika na prethodni ekran
                }
                is EventDetailsEvent.DeletionError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.event?.name ?: "Event Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isOwner) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Event", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            // Prikazujemo FAB-ove samo ako je događaj uspešno učitan
            if (state.event != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Edit dugme se prikazuje samo ako je korisnik vlasnik
                    if (isOwner) {
                        FloatingActionButton(onClick = { onNavigateToEditEvent(state.event!!.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Event")
                        }
                    }
                    // Show on Map dugme je uvek tu
                    FloatingActionButton(
                        onClick = { onNavigateToMap(LatLng(state.event!!.location.latitude, state.event!!.location.longitude)) }
                    ) {
                        Icon(Icons.Default.Map, contentDescription = "Show on Map")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Text(state.error!!, modifier = Modifier.align(Alignment.Center))
                state.event != null -> {
                    // Sada 'EventDetailsContent' nema Scaffold i prima samo 'event' i 'onCreatorClick'
                    EventDetailsContent(
                        event = state.event!!,
                        onCreatorClick = onCreatorClick
                    )
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to permanently delete this event? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onDeleteEvent()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

}



@Composable
fun EventDetailsContent(
    event: Event,
    onCreatorClick: (String)-> Unit
) {
    val dateFormatter = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
    println("DETAILS_DEBUG: Displaying event: $event")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Naslov i kategorija
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = event.name, style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            CategoryChip(category = EventCategory.fromString(event.category))
            Spacer(modifier = Modifier.height(8.dp))

            // Detalji sa ikonicama
            DetailRow(
                icon = Icons.Default.Event,
                text = event.eventTimestamp?.toDate()?.let { dateFormatter.format(it) } ?: "N/A"
            )
            DetailRow(
                icon = Icons.Default.LocationOn,
                text = "Location (Lat: ${event.location.latitude}, Lng: ${event.location.longitude})"
            )
            Row(
                modifier = Modifier.clickable { onCreatorClick(event.creatorId) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = stringResource(id = R.string.event_created_by, event.creatorName), style = MaterialTheme.typography.bodyLarge)
            }
            DetailRow(
                icon = Icons.Default.AttachMoney,
                text = if (event.free)
                    stringResource(id = R.string.event_price_free)
                else
                    stringResource(id = R.string.event_price_format, event.price)
            )
            DetailRow(
                icon = Icons.Default.NoAdultContent,
                text = if (event.ageRestriction > 0)
                    stringResource(id = R.string.event_age_restriction_present)
                else
                    stringResource(id = R.string.event_age_restriction_absent)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(id = R.string.event_about_label),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = event.description, style = MaterialTheme.typography.bodyLarge)
        }

    }
}


@Composable
fun DetailRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
    Spacer(modifier = Modifier.height(8.dp))
}

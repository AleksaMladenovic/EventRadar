package com.eventradar.ui.event_details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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

@Composable
fun EventDetailsScreen(
    viewModel: EventDetailsViewModel = hiltViewModel(),
    onNavigateToMap: (LatLng) -> Unit,
    onNavigateBack: () -> Unit,
    onCreatorClick: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Kreiramo lokalnu, nepromenljivu kopiju
    val error = state.error

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    Text(
                        text = stringResource(id = R.string.event_details_loading),
                        modifier = Modifier.align(Alignment.Center).padding(top = 64.dp)
                    )
                }

                error != null -> {
                    Text(
                        text = error,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                state.event != null -> {
                    // Ako je sve u redu, prikaži detalje
                    EventDetailsContent(event = state.event!!, onShowMap = onNavigateToMap, onCreatorClick = onCreatorClick)
                }
            }
        }

}

@Composable
fun EventDetailsContent(
    event: Event,
    onShowMap: (LatLng) -> Unit,
    onCreatorClick: (String)-> Unit
) {
    val dateFormatter = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
    val latLng = LatLng(event.location.latitude, event.location.longitude)

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
                text = if (event.isFree)
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

        // FloatingActionButton dole desno
        FloatingActionButton(
            onClick = { onShowMap(latLng) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = stringResource(R.string.show_on_map)
            )
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

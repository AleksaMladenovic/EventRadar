package com.eventradar.ui.event_details

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.eventradar.data.model.CommentWithAuthor
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
    val event = state.event
    val isOwner = event?.let { viewModel.isCurrentUserOwner(it.creatorId) } ?: false
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
                title = { Text(event?.name ?: "Event Details") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    if (isOwner) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete Event", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (event != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (isOwner) {
                        FloatingActionButton(onClick = { onNavigateToEditEvent(event.id) }) { Icon(Icons.Default.Edit, "Edit") }
                    }
                    FloatingActionButton(onClick = { onNavigateToMap(LatLng(event.location.latitude, event.location.longitude)) }) {
                        Icon(Icons.Default.Map, "Show on Map")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Padding od TopAppBar-a
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Text(state.error!!, modifier = Modifier.align(Alignment.Center))
                event != null -> {
                    // EventDetailsContent je sada ponovo JEDNOSTAVAN
                    EventDetailsContent(
                        event = event,
                        isCurrentUserAttending = state.isCurrentUserAttending,
                        onToggleAttendance = { viewModel.onToggleAttendanceClick() },
                        onCreatorClick = onCreatorClick,
                        comments = state.comments,
                        onAddComment = { text -> viewModel.onAddComment(text) }
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
private fun EventDetailsContent(
    event: Event,
    isCurrentUserAttending: Boolean,
    onToggleAttendance: () -> Unit,
    onCreatorClick: (String) -> Unit,
    comments: List<CommentWithAuthor>,
    onAddComment: (String) -> Unit
) {
    // SADA JE SVE UNUTAR JEDNOG LAZYCOLUMN-A
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp) // Padding za ceo sadržaj
    ) {
        // Stavka 1: Sve informacije o događaju
        item {
            EventInfoSection(
                event = event,
                isCurrentUserAttending = isCurrentUserAttending,
                onToggleAttendance = onToggleAttendance,
                onCreatorClick = onCreatorClick
            )
        }

        // Stavka 2: Naslov "Comments"
        item {
            Text(
                text = "Comments (${comments.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
        }

        // Stavke 3: Lista komentara
        items(items = comments, key = { it.comment.id }) { commentWithAuthor ->
            CommentItem(commentWithAuthor = commentWithAuthor)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // Stavka 4: Polje za unos novog komentara
        item {
            CommentInput(onCommentSend = onAddComment)
        }

        // --- KLJUČNO REŠENJE ---
        // Stavka 5: Prazan prostor na dnu liste
        // Visina je dovoljna da stane FAB (obično 56dp) plus dodatni padding (16dp).
        item {
            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}



@Composable
private fun EventInfoSection(
    event: Event,
    isCurrentUserAttending: Boolean,
    onToggleAttendance: () -> Unit,
    onCreatorClick: (String) -> Unit
) {
    val dateFormatter = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CategoryChip(category = EventCategory.fromString(event.category))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = event.name, style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Dugme za prisustvo
        Button(onClick = onToggleAttendance, modifier = Modifier.fillMaxWidth()) {
            if (isCurrentUserAttending) {
                Icon(Icons.Default.Check, contentDescription = "You are going")
                Spacer(modifier = Modifier.width(8.dp))
                Text("You're Going!")
            } else {
                Text("I'm Going")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${event.attendeeIds.size} people are going",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Ostali detalji (sada lepo poravnati levo)
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            DetailRow(icon = Icons.Default.Event, text = event.eventTimestamp?.toDate()?.let { dateFormatter.format(it) } ?: "N/A")
            DetailRow(icon = Icons.Default.LocationOn, text = "Location (Lat: ${event.location.latitude}, Lng: ${event.location.longitude})")
            Row(modifier = Modifier.clickable { onCreatorClick(event.creatorId) }, /* ... */) { /* ... */ }
            // ... ostali DetailRow ...
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = stringResource(id = R.string.event_about_label), style = MaterialTheme.typography.titleMedium)
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

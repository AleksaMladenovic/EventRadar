package com.eventradar.ui.event_details

import androidx.compose.ui.graphics.Color
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text(event?.name ?: "Event Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Akcija "Show on Map" je uvek vidljiva
                    if (event != null) {
                        IconButton(onClick = { onNavigateToMap(LatLng(event.location.latitude, event.location.longitude)) }) {
                            Icon(Icons.Default.Map, contentDescription = "Show on Map")
                        }
                    }

                    // Akcije "Edit" i "Delete" su vidljive samo vlasniku
                    if (isOwner) {
                        IconButton(onClick = { onNavigateToEditEvent(event.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Event")
                        }
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Event", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
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
                    EventDetailsContent(
                        event = event,
                        isCurrentUserAttending = state.isCurrentUserAttending,
                        onToggleAttendance = { viewModel.onToggleAttendanceClick() },
                        onCreatorClick = onCreatorClick,
                        comments = state.comments,
                        onAddComment = { text -> viewModel.onAddComment(text) },
                        averageRating = state.averageRating,
                        currentUserRating = state.currentUserRating,
                        onRatingChanged = {newValue -> viewModel.onRatingChanged(newValue)},

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
    onAddComment: (String) -> Unit,
    averageRating:Float,
    currentUserRating: Int,
    onRatingChanged: (Int)->Unit,
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Comments", "Rating")

    // Column sada samo drži polje za unos, a lista je iznad njega
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f) // Zauzima sav prostor OSIM za CommentInput
        ) {
            // Stavka 1: Informacije o događaju
            item {
                EventInfoSection(
                    event = event,
                    isCurrentUserAttending = isCurrentUserAttending,
                    onToggleAttendance = onToggleAttendance,
                    onCreatorClick = onCreatorClick,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Stavka 2: Tabovi
            item {
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(text = title) }
                        )
                    }
                }
            }

            // Stavka 3: Sadržaj tabova
            item {
                when (selectedTabIndex) {
                    0 -> { // Comments
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (comments.isEmpty()) {
                                Text("No comments yet. Be the first to comment!", modifier = Modifier.padding(vertical = 16.dp))
                            }
                            // Lista komentara
                            comments.forEach { commentWithAuthor ->
                                CommentItem(commentWithAuthor = commentWithAuthor)
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    thickness = DividerDefaults.Thickness,
                                    color = DividerDefaults.color
                                )
                            }
                        }
                    }
                    1 -> { // Rating
                        RatingSection(
                            averageRating = averageRating,
                            ratingCount = event.ratingCount,
                            currentUserRating = currentUserRating,
                            onRatingChanged = onRatingChanged,
                        )
                    }
                }
            }
        }

        // Polje za unos komentara je uvek na dnu, ali samo ako je "Comments" tab selektovan
        if (selectedTabIndex == 0) {
            CommentInput(onCommentSend = onAddComment)
        }
    }
}


@Composable
private fun RatingSection(
    averageRating: Float,
    ratingCount: Long,
    currentUserRating: Int,
    onRatingChanged: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Average Rating", style = MaterialTheme.typography.titleMedium)
        Text(
            text = String.format(Locale.US, "%.1f", averageRating),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold
        )
        Text("Based on $ratingCount ratings", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(32.dp))

        Text("Your Rating", style = MaterialTheme.typography.titleMedium)
        RatingBar(
            currentRating = currentUserRating,
            onRatingChanged = onRatingChanged
        )
    }
}

@Composable
fun RatingBar(
    currentRating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starCount: Int = 5,
    starColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        (1..starCount).forEach { starIndex ->
            val isSelected = starIndex <= currentRating
            Icon(
                imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "Star $starIndex",
                tint = if (isSelected) starColor else Color.Gray,
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onRatingChanged(starIndex) }
            )
        }
    }
}

@Composable
private fun EventInfoSection(
    modifier: Modifier = Modifier,
    event: Event,
    isCurrentUserAttending: Boolean,
    onToggleAttendance: () -> Unit,
    onCreatorClick: (String) -> Unit
) {
    val dateFormatter = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
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
        val dateFormatter = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {

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
            Spacer(modifier = Modifier.height(8.dp))
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
            Spacer(modifier = Modifier.height(16.dp))
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
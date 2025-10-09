package com.eventradar.ui.events_list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eventradar.R
import androidx.compose.ui.unit.sp
import com.eventradar.data.model.Event
import com.eventradar.data.model.EventCategory
import com.eventradar.ui.components.CategoryChip
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EventListItem(
    event: Event,
    onItemClick: (Event) -> Unit
) {
    val dateFormatter = SimpleDateFormat("dd.MM.yyyy 'at' HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick(event) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // Sada koristimo Column kao glavni kontejner da bi čip bio iznad naslova
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Kolona za glavni sadržaj
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kolona za tekstualne informacije
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleLarge, // Malo veći naslov
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = event.eventTimestamp?.toDate()?.let { dateFormatter.format(it) } ?: stringResource(id = R.string.date_not_set),
                            style = MaterialTheme.typography.bodyMedium, // Malo veći body
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        CategoryChip(category = EventCategory.fromString(event.category))

                    }

                }
            }

            Spacer(modifier = Modifier.height(12.dp))

        }
    }
}





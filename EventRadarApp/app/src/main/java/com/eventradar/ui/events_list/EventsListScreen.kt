package com.eventradar.ui.events_list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventradar.R

@Composable
fun EventsListScreen(
    // Lambda za navigaciju ka detaljima događaja
    onNavigateToEventDetails: (String) -> Unit,
    // ViewModel se kreira automatski pomoću Hilt-a
    viewModel: EventsListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                // Prikazujemo indikator dok se podaci učitavaju
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            state.error != null -> {
                // Prikazujemo poruku o grešci
                Text(
                    text = state.error!!,
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            }
            state.events.isEmpty() -> {
                // Prikazujemo poruku ako nema događaja
                Text(
                    text = stringResource(id = R.string.no_events_found),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                // Prikazujemo listu događaja
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp) // Razmak između kartica
                ) {
                    // 'items' je optimizovana funkcija za prikazivanje listi
                    items(
                        items = state.events,
                        key = { event -> event.id } // Ključ za bolju performansu pri promenama
                    ) { event ->
                        EventListItem(
                            event = event,
                            onItemClick = {
                                // Kada se klikne na stavku, pozivamo navigaciju
                                onNavigateToEventDetails(event.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

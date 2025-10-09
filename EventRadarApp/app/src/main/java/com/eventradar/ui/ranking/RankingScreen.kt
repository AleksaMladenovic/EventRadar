package com.eventradar.ui.ranking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventradar.R

@Composable
fun RankingScreen(
    viewModel: RankingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            state.error != null -> {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }
            state.topThreeUsers.isEmpty() && state.otherUsers.isEmpty() -> {
                Text(
                    text = stringResource(id = R.string.no_users_found_in_ranking),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Stavka 1: PODIJUM
                    if (state.topThreeUsers.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Drugo mesto (levo)
                                if (state.topThreeUsers.size >= 2) {
                                    PodiumItem(
                                        user = state.topThreeUsers[1],
                                        rank = 2,
                                        size = 80.dp,
                                        borderColor = Color(0xFFC0C0C0) // Srebrna
                                    )
                                } else {
                                    // Prazan prostor da bi prvo mesto bilo u sredini ako nema drugog
                                    Spacer(modifier = Modifier.width(80.dp + 16.dp))
                                }

                                // Prvo mesto (u sredini, veće)
                                PodiumItem(
                                    user = state.topThreeUsers[0],
                                    rank = 1,
                                    size = 100.dp,
                                    borderColor = Color(0xFFFFD700) // Zlatna
                                )

                                // Treće mesto (desno)
                                if (state.topThreeUsers.size >= 3) {
                                    PodiumItem(
                                        user = state.topThreeUsers[2],
                                        rank = 3,
                                        size = 80.dp,
                                        borderColor = Color(0xFFCD7F32) // Bronzana
                                    )
                                } else {
                                    // Prazan prostor da bi prvo mesto bilo u sredini ako nema trećeg
                                    Spacer(modifier = Modifier.width(80.dp + 16.dp))
                                }
                            }
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }

                    // Naslov za ostatak liste
                    if (state.otherUsers.isNotEmpty()) {
                        item {
                            Text(
                                text = "Leaderboard", // Dodaj u strings.xml
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                            )
                        }
                    }

                    // Ostale stavke: Lista korisnika od 4. mesta pa naniže
                    itemsIndexed(
                        items = state.otherUsers,
                        key = { _, user -> user.uid }
                    ) { index, user ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            RankingListItem(
                                user = user,
                                rank = index + 4 // Počinjemo od 4. mesta
                            )
                        }
                    }
                }
            }
        }
    }
}
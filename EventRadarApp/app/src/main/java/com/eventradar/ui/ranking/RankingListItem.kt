package com.eventradar.ui.ranking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.eventradar.data.model.User
import com.eventradar.R

@Composable
fun RankingListItem(
    user: User,
    rank: Int // Rang korisnika (1, 2, 3...)
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prikaz ranga
            Text(
                text = "$rank.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp) // Fiksna širina za poravnanje
            )

            // Slika profila
            AsyncImage(
                model = user.profileImageUrl,
                contentDescription = stringResource(id = R.string.profile_picture_description, user.username),
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                // Slika koja se prikazuje ako korisnik nema sliku
                fallback = painterResource(id = R.drawable.ic_default_avatar)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Ime i prezime
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${user.firstName} ${user.lastName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Poeni
            Text(
                text = stringResource(id = R.string.points_format, user.points),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun RankingListItemPreview() {
        val previewUser = User(
            firstName = "Petar",
            lastName = "Petrović",
            username = "pera",
            points = 1250
        )
        RankingListItem(user = previewUser, rank = 1)
}

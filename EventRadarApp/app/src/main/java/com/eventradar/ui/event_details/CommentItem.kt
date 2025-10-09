package com.eventradar.ui.event_details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.eventradar.R
import com.eventradar.data.model.Comment
import com.eventradar.data.model.CommentWithAuthor
import com.eventradar.data.model.User
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CommentItem(commentWithAuthor: CommentWithAuthor) {
    val author = commentWithAuthor.author
    val comment = commentWithAuthor.comment
    val dateFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar autora
        AsyncImage(
            model = author?.profileImageUrl,
            contentDescription = stringResource(id = R.string.avatar_of, author?.username ?: ""),
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            fallback = painterResource(id = R.drawable.ic_default_avatar)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Kolona za ime, vreme i tekst komentara
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${author?.firstName ?: stringResource(id = R.string.unknown)} ${author?.lastName ?: stringResource(id = R.string.user_label)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = comment.timestamp?.toDate()?.let { dateFormatter.format(it) } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}



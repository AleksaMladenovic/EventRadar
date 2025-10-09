package com.eventradar.ui.ranking

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.eventradar.R
import com.eventradar.data.model.User

@Composable
fun PodiumItem(
    user: User,
    rank: Int,
    size: Dp,
    borderColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        // Ikonica krune/medalje za prvo mesto
        if (rank == 1) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = stringResource(id = R.string.first_place),
                tint = Color(0xFFFFD700), // Zlatna boja
                modifier = Modifier.size(24.dp)
            )
        } else {
            // Prazan prostor da bi se imena poravnala
            Spacer(modifier = Modifier.height(24.dp))
        }

        AsyncImage(
            model = user.profileImageUrl,
            contentDescription = stringResource(id = R.string.avatar),
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(2.dp, borderColor, CircleShape),
            contentScale = ContentScale.Crop,
            fallback = painterResource(id = R.drawable.ic_default_avatar)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = user.username, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(text = stringResource(id = R.string.points_format, user.points), color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
    }
}

package com.eventradar.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.eventradar.R
import com.eventradar.data.model.User

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToMyEvents : (String) -> Unit,
    onNavigateToAttendingEvents : (String) -> Unit,
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
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            state.user != null -> {
                ProfileContent(
                    user = state.user!!,
                    onSignOut = { viewModel.signOut() },
                    onNavigateToMyEvents,
                    onNavigateToAttendingEvents,
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    user: User,
    onSignOut: () -> Unit,
    onNavigateToMyEvents : (String) -> Unit,
    onNavigateToAttendingEvents : (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Slika profila
        AsyncImage(
            model = user.profileImageUrl,
            contentDescription = stringResource(
                id = R.string.profile_picture_description,
                user.username
            ),

            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            fallback = painterResource(id = R.drawable.ic_default_avatar)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Ime i korisničko ime
        Text(
            text = "${user.firstName} ${user.lastName}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "@${user.username}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Poeni
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = user.points.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(id = R.string.profile_points),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Ostali detalji
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        ProfileInfoRow(icon = Icons.Default.Email, text = user.email)
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        ProfileInfoRow(icon = Icons.Default.Phone, text = user.phone)
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        Spacer(modifier = Modifier.weight(1f)) // Gura dugme na dno

        // Dugme za moje event-e
        Button(onClick = {onNavigateToMyEvents(user.uid)}) {
            Text(stringResource(id = R.string.my_events))
        }

        // Dugme za prijavljene evente
        OutlinedButton(onClick = {onNavigateToAttendingEvents(user.uid)}) {
            Text(stringResource(id = R.string.events_im_attending))
        }
        // Dugme za odjavu
        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text(stringResource(id = R.string.profile_sign_out))
        }
    }
}

@Composable
private fun ProfileInfoRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

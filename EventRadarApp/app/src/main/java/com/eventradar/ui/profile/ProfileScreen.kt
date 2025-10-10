package com.eventradar.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Lock
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToMyEvents: (String) -> Unit,
    onNavigateToAttendingEvents: (String) -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Text(state.error!!, modifier = Modifier.align(Alignment.Center))
                state.user != null -> {
                    ProfileContent(
                        user = state.user!!,
                        onSignOut = { viewModel.signOut() },
                        onNavigateToMyEvents = onNavigateToMyEvents,
                        onNavigateToAttendingEvents = onNavigateToAttendingEvents,
                        onNavigateToEditProfile = onNavigateToEditProfile,
                        onNavigateToChangePassword = onNavigateToChangePassword
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    user: User,
    onSignOut: () -> Unit,
    onNavigateToMyEvents: (String) -> Unit,
    onNavigateToAttendingEvents: (String) -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToChangePassword: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp), // Padding samo gore i dole
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- SEKCIJA 1: Korisnički Info ---
        ProfileHeader(user = user)

        Spacer(modifier = Modifier.height(24.dp))

        // --- SEKCIJA 2: Akcije vezane za događaje ---
        SectionTitle(title = "My Activity") // Dodaj u strings.xml
        ActionRow(
            icon = Icons.Default.ListAlt,
            text = stringResource(id = R.string.my_events),
            onClick = { onNavigateToMyEvents(user.uid) }
        )
        ActionRow(
            icon = Icons.Default.EventAvailable,
            text = stringResource(id = R.string.events_im_attending),
            onClick = { onNavigateToAttendingEvents(user.uid) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- SEKCIJA 3: Podešavanja naloga ---
        SectionTitle(title = "Account Settings") // Dodaj u strings.xml
        ActionRow(
            icon = Icons.Default.Edit,
            text = stringResource(id = R.string.edit_profile_title),
            onClick = onNavigateToEditProfile
        )
        ActionRow(
            icon = Icons.Default.Lock,
            text = stringResource(R.string.change_password),
            onClick = onNavigateToChangePassword
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- SEKCIJA 4: Odjava ---
        Button(
            onClick = onSignOut,
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text(stringResource(id = R.string.profile_sign_out))
        }
    }
}

@Composable
private fun ProfileHeader(user: User) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = user.profileImageUrl,
            contentDescription = stringResource(id = R.string.profile_picture_description, user.username),
            modifier = Modifier.size(120.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
            fallback = painterResource(id = R.drawable.ic_default_avatar)
        )
        Spacer(modifier = Modifier.height(16.dp))
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
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
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
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = text, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
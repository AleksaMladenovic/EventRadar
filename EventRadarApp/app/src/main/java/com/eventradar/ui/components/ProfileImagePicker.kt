package com.eventradar.ui.components

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import java.util.Objects
import com.eventradar.R

@Composable
fun ProfileImagePicker(
    modifier: Modifier = Modifier,
    imageUri: Uri?,
    onImageSelected: (Uri?) -> Unit,
    size: Dp = 120.dp
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    // --- Launcheri za dobijanje slike ---

    // 1. Launcher za galeriju
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        println("IMAGE_PICKER: Gallery returned URI: $uri")
        onImageSelected(uri)
    }

    // 2. Launcher za kameru
    // Prvo kreiramo privremeni fajl i URI gde će kamera sačuvati sliku
    val tempUri = remember {
        val tempFile = File.createTempFile(
            "avatar_",
            ".jpg",
            context.cacheDir
        ).apply {
            createNewFile()
        }
        FileProvider.getUriForFile(
            Objects.requireNonNull(context),
            "${context.packageName}.provider",
            tempFile
        )
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            println("IMAGE_PICKER: Camera returned success. URI should be: $tempUri")
            onImageSelected(tempUri)
        }
    }

    // 3. Launcher za traženje dozvole za kameru
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Ako je dozvola data, pokrećemo kameru
            cameraLauncher.launch(tempUri)
        } else {
            // Opciono: Pokaži poruku korisniku da je dozvola odbijena
             Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Dijalog za izbor izvora ---
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.image_picker_title)) },
            text = { Text(stringResource(R.string.image_picker_description)) },
            confirmButton = {
                Button(onClick = {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    showDialog = false
                }) {
                    Text(stringResource(R.string.image_picker_camera))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { // Stavio sam OutlinedButton radi lepšeg dizajna
                    galleryLauncher.launch("image/*")
                    showDialog = false
                }) {
                    Text(stringResource(R.string.image_picker_gallery))
                }
            }
        )
    }

    // --- UI za prikaz avatara ---
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { showDialog = true },
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            // Koristimo AsyncImage iz Coil biblioteke za učitavanje slike
            AsyncImage(
                model = imageUri,
                contentDescription = stringResource(R.string.cd_profile_image),
                modifier = Modifier.fillMaxSize(), // Promenjeno u fillMaxSize
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.AddAPhoto,
                contentDescription = stringResource(R.string.cd_add_profile_image),
                modifier = Modifier.size(size / 3),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
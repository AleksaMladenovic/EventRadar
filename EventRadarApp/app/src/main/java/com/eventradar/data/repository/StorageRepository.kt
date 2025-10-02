package com.eventradar.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton


@Singleton // Kažemo Hilt-u da treba da postoji samo jedna instanca ovog repozitorijuma
class StorageRepository @Inject constructor(
    private val storage: FirebaseStorage // Hilt će nam dati instancu FirebaseStorage-a
) {
    suspend fun uploadProfileImage(imageUri: Uri): Result<String> {
        println("STORAGE_REPO: Starting image upload for URI: $imageUri")
        return try {
            // Kreiramo jedinstveno ime fajla, npr. "profile_images/neki-random-uuid.jpg"
            val fileName = "profile_images/${UUID.randomUUID()}"
            val storageRef = storage.reference.child(fileName)

            // Upload-ujemo fajl i čekamo da se završi
            storageRef.putFile(imageUri).await()

            // Dobavljamo URL za download i čekamo da se završi
            val downloadUrl = storageRef.downloadUrl.await()
            println("STORAGE_REPO: Upload successful. Download URL: $downloadUrl")
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            println("STORAGE_REPO: UPLOAD FAILED! Exception: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
package com.eventradar.data.repository

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class CloudinaryRepository @Inject constructor() {


    suspend fun uploadProfileImage(uri: Uri): Result<String> = suspendCoroutine { continuation ->

        MediaManager.get().upload(uri)
            .option("upload_preset", "android_preset")
            .callback(object : UploadCallback {

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val url = resultData?.get("secure_url") as? String
                    if (url != null) {
                        println("CLOUDINARY: Upload successful. URL: $url")
                        continuation.resume(Result.success(url))
                    } else {
                        println("CLOUDINARY: Upload failed. URL not found in result.")
                        continuation.resume(Result.failure(Exception("Cloudinary URL not found")))
                    }
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    println("CLOUDINARY: Upload error: ${error?.description}")
                    continuation.resume(Result.failure(Exception(error?.description ?: "Unknown Cloudinary error")))
                }

                // Ove metode nam ne trebaju, ali moraju biti implementirane
                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                override fun onStart(requestId: String?) {
                    println("CLOUDINARY: Starting upload...")
                }
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
            })
            .dispatch() // Pokreni upload
    }
}
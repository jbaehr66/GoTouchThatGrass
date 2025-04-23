package com.example.gotouchthatgrass_3.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class GrassDetector(private val context: Context) {
    private var mlKitAvailable = true

    init {
        try {
            ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        } catch (e: Exception) {
            Log.w("GrassDetector", "ML Kit not available: ${e.message}")
            mlKitAvailable = false
        }
    }

    suspend fun isTouchingGrass(bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
        if (!mlKitAvailable) return@withContext false

        val image = InputImage.fromBitmap(bitmap, 0)
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        suspendCancellableCoroutine<Boolean> { continuation ->
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    val hasGrass = labels.any { it.text.equals("grass", true) && it.confidence > 0.75 }
                    val hasHand = labels.any { (it.text.equals("hand", true) || it.text.contains("arm", true)) && it.confidence > 0.75 }
                    continuation.resume(hasGrass && hasHand)
                }
                .addOnFailureListener {
                    continuation.resume(false)
                }
        }
    }
}

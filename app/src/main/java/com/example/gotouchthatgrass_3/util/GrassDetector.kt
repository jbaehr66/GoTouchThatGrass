package com.example.gotouchthatgrass_3.util

// util/GrassDetector.kt

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume

class GrassDetector(private val context: Context) {
    private var mlKitAvailable = true
    
    init {
        // Check if ML Kit is available
        try {
            ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        } catch (e: Exception) {
            Log.w("GrassDetector", "ML Kit not available: ${e.message}")
            mlKitAvailable = false
        }
    }

    // List of labels that are associated with grass
    private val grassLabels = listOf(
        "grass", "lawn", "plant", "vegetation", "field", "meadow", "pasture"
    )

    // Basic color detection for green (simple approach)
    fun hasGreenColor(bitmap: Bitmap, threshold: Float = 0.3f): Boolean {
        var greenPixelCount = 0
        val totalPixels = bitmap.width * bitmap.height

        // Sample pixels
        for (x in 0 until bitmap.width step 10) {
            for (y in 0 until bitmap.height step 10) {
                val pixel = bitmap.getPixel(x, y)
                val red = (pixel shr 16) and 0xff
                val green = (pixel shr 8) and 0xff
                val blue = pixel and 0xff

                // Check if the green channel is dominant
                if (green > red * 1.2 && green > blue * 1.2) {
                    greenPixelCount++
                }
            }
        }

        val sampledPixels = (bitmap.width / 10) * (bitmap.height / 10)
        return greenPixelCount.toFloat() / sampledPixels > threshold
    }

    // ML Kit Image Labeling to detect grass
    suspend fun detectGrassUsingML(imageBitmap: Bitmap): Boolean = withContext(Dispatchers.Default) {
        suspendCancellableCoroutine { continuation ->
            try {
                val inputImage = InputImage.fromBitmap(imageBitmap, 0)
                val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

                labeler.process(inputImage)
                    .addOnSuccessListener { labels ->
                        val detectedGrass = labels.any { label ->
                            grassLabels.any { grassType ->
                                label.text.lowercase().contains(grassType) && label.confidence > 0.6f
                            }
                        }
                        continuation.resume(detectedGrass)
                    }
                    .addOnFailureListener {
                        // Fallback to basic color detection if ML fails
                        continuation.resume(hasGreenColor(imageBitmap))
                    }
            } catch (e: IOException) {
                continuation.resume(hasGreenColor(imageBitmap))
            }
        }
    }

    // Combined approach - uses both color detection and ML with improved accuracy
    suspend fun isGrassInImage(imageBitmap: Bitmap): Boolean {
        // Always check with basic color detection
        val hasGreenDominance = hasGreenColor(imageBitmap, threshold = 0.25f)
        
        // Only try ML detection if available
        val mlDetection = if (mlKitAvailable) {
            try {
                detectGrassUsingML(imageBitmap)
            } catch (e: Exception) {
                // Log the error but don't crash
                e.printStackTrace()
                false
            }
        } else false

        // If either method detects grass with high confidence, consider it valid
        // This makes the app more user-friendly while still maintaining the challenge
        return hasGreenDominance || mlDetection
    }
}
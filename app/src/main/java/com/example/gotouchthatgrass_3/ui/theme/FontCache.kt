package com.example.gotouchthatgrass_3.ui.theme

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.gotouchthatgrass_3.R

/**
 * Enhanced font cache to avoid repeated disk reads when loading typefaces
 * with robust error handling and fallbacks
 */
object FontCache {
    private val cache = mutableMapOf<Int, Typeface?>()
    private val failedFonts = mutableSetOf<Int>()
    private val nameCache = mutableMapOf<String, Int>()

    // Fallback system for looking up font IDs by name when resource IDs fail
    private fun getFontResourceByName(context: Context, fontName: String): Int? {
        // Check if already in cache
        if (nameCache.containsKey(fontName)) {
            return nameCache[fontName]
        }
        
        // Try to resolve the resource ID by name
        try {
            val resourceId = context.resources.getIdentifier(
                fontName, "font", context.packageName
            )
            if (resourceId != 0) {
                nameCache[fontName] = resourceId
                return resourceId
            }
        } catch (e: Exception) {
            Log.e("FontCache", "Error resolving font resource by name: $fontName", e)
        }
        
        return null
    }

    // Map of known font names to their fallback hardcoded font resource IDs
    private val knownFonts = mapOf(
        "poppins_regular" to R.font.poppins_regular,
        "poppins_medium" to R.font.poppins_medium,
        "poppins_bold" to R.font.poppins_bold,
        "poppins_light" to R.font.poppins_light,
        "poppins_regular_font" to R.font.poppins_regular,
        "poppins_medium_font" to R.font.poppins_medium,
        "poppins_bold_font" to R.font.poppins_bold,
        "poppins_light_font" to R.font.poppins_light
    )

    // Get the font style name based on resource ID (for debugging)
    private fun getFontName(resId: Int): String {
        return when (resId) {
            R.font.poppins_regular -> "poppins_regular"
            R.font.poppins_medium -> "poppins_medium"
            R.font.poppins_bold -> "poppins_bold"
            R.font.poppins_light -> "poppins_light"
            else -> "unknown_font_$resId"
        }
    }

    /**
     * Safely get font from cache or load it, with error handling and fallback mechanisms
     */
    fun getFont(context: Context, fontResId: Int): Typeface? {
        // Check if we already know this font fails to load
        if (failedFonts.contains(fontResId)) {
            Log.d("FontCache", "Using fallback for known failing font: ${getFontName(fontResId)}")
            return Typeface.DEFAULT
        }
        
        // Return from cache if available
        cache[fontResId]?.let { return it }
        
        // Try to load font with multiple fallback strategies
        try {
            // First attempt: direct resource load
            try {
                val typeface = ResourcesCompat.getFont(context, fontResId)
                if (typeface != null) {
                    Log.d("FontCache", "Successfully loaded font: ${getFontName(fontResId)}")
                    cache[fontResId] = typeface
                    return typeface
                }
            } catch (e: Resources.NotFoundException) {
                // Font resource not found, will try fallbacks
                Log.w("FontCache", "Font not found: $fontResId (${getFontName(fontResId)})", e)
            }
            
            // Second attempt: try finding by font name
            val fontName = getFontName(fontResId)
            val resolvedId = getFontResourceByName(context, fontName)
            if (resolvedId != null && resolvedId != fontResId) {
                try {
                    val typeface = ResourcesCompat.getFont(context, resolvedId)
                    if (typeface != null) {
                        Log.d("FontCache", "Loaded font via name lookup: $fontName -> $resolvedId")
                        cache[fontResId] = typeface  // Cache with original ID
                        return typeface
                    }
                } catch (e: Exception) {
                    Log.w("FontCache", "Failed to load font by name: $fontName", e)
                }
            }
            
            // Last resort: use system default
            Log.w("FontCache", "Using system default font as fallback for: $fontResId")
            failedFonts.add(fontResId)  // Remember it failed
            return Typeface.DEFAULT
            
        } catch (e: Exception) {
            Log.e("FontCache", "Error loading font: $fontResId (${getFontName(fontResId)})", e)
            failedFonts.add(fontResId)
            return Typeface.DEFAULT
        }
    }

    /**
     * Preload a list of font resource IDs with advanced diagnostic logging
     */
    fun preload(context: Context, fontResIds: List<Int>) {
        // Start on a background thread
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("FontCache", "Preloading ${fontResIds.size} fonts")
            logResourceInfo(context)
            
            // Track success/failure stats
            var successes = 0
            var failures = 0
            
            // Try each font individually
            fontResIds.forEach { id ->
                val fontName = getFontName(id)
                Log.d("FontCache", "Attempting to preload font: $fontName (ID: $id)")
                
                try {
                    if (!failedFonts.contains(id) && !cache.containsKey(id)) {
                        val typeface = getFont(context, id)
                        if (typeface != null) {
                            successes++
                        } else {
                            failures++
                        }
                    }
                } catch (e: Exception) {
                    failures++
                    Log.e("FontCache", "Failed to preload font: $id ($fontName)", e)
                }
            }
            
            Log.d("FontCache", "Font preloading complete: $successes loaded, $failures failed")
        }
    }
    
    /**
     * Helper method to get system default font
     */
    fun getSystemFont(): Typeface {
        return Typeface.DEFAULT
    }
    
    /**
     * Log diagnostic information about the current resource environment
     */
    private fun logResourceInfo(context: Context) {
        try {
            Log.d("FontCache", "App package: ${context.packageName}")
            
            // Log known font resource IDs
            Log.d("FontCache", "Font resource IDs:")
            Log.d("FontCache", "  poppins_regular: ${R.font.poppins_regular}")
            Log.d("FontCache", "  poppins_medium: ${R.font.poppins_medium}")
            Log.d("FontCache", "  poppins_bold: ${R.font.poppins_bold}")
            Log.d("FontCache", "  poppins_light: ${R.font.poppins_light}")
            
            // Try resolving font names
            val fontNames = listOf("poppins_regular", "poppins_medium", "poppins_bold", "poppins_light")
            fontNames.forEach { name ->
                val id = context.resources.getIdentifier(name, "font", context.packageName)
                Log.d("FontCache", "  Resolved '$name' to: $id")
            }
        } catch (e: Exception) {
            Log.e("FontCache", "Error logging resource info", e)
        }
    }
}
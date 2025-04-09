package com.example.gotouchthatgrass_3.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Extension property to check if a view is attached to window
 * Used to prevent updating views that are no longer attached
 */
val View.isAttachedToWindow: Boolean
    get() = windowToken != null

/**
 * Utility class for loading and caching fonts to avoid UI thread disk operations
 */
object FontLoader {
    // Cache to store loaded typefaces
    private val typefaceCache = ConcurrentHashMap<Int, Typeface?>()
    
    /**
     * Loads a font from resources on a background thread and applies it to a TextView
     * @param lifecycleScope The coroutine scope to launch in
     * @param context Application context
     * @param textView TextView to apply the font to
     * @param fontResId The font resource ID from R.font
     */
    fun loadFontAsync(
        lifecycleScope: LifecycleCoroutineScope,
        context: Context,
        textView: TextView,
        fontResId: Int
    ) {
        try {
            // Safety checks
            if (textView.visibility == TextView.GONE) return
            if (context == null || textView == null) return
            
            // First check cache - this is a safe main thread operation
            val cachedTypeface = typefaceCache[fontResId]
            if (cachedTypeface != null) {
                try {
                    textView.typeface = cachedTypeface
                } catch (e: Exception) {
                    Log.e("FontLoader", "Error setting cached typeface", e)
                }
                return
            }
            
            // Load on background thread
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Double-check if it was loaded while we were waiting
                    var typeface = typefaceCache[fontResId]
                    if (typeface == null) {
                        try {
                            // Load from resources (disk operation)
                            typeface = ResourcesCompat.getFont(context, fontResId)
                            
                            // Only cache valid typefaces
                            if (typeface != null) {
                                typefaceCache[fontResId] = typeface
                            } else {
                                Log.w("FontLoader", "Null typeface returned for font: $fontResId")
                                return@launch
                            }
                        } catch (resourceException: Exception) {
                            Log.e("FontLoader", "Resource error loading font: $fontResId", resourceException)
                            return@launch
                        }
                    }
                    
                    // Apply on main thread
                    withContext(Dispatchers.Main) {
                        try {
                            // Safety check to ensure TextView is still valid
                            if (textView.isAttachedToWindow) {
                                textView.typeface = typeface
                            }
                        } catch (e: Exception) {
                            Log.e("FontLoader", "Failed to set typeface on main thread", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FontLoader", "Failed to load font: $fontResId", e)
                }
            }
        } catch (e: Exception) {
            // Catch any exceptions to avoid crashes in font loading
            Log.e("FontLoader", "Unexpected error in loadFontAsync", e)
        }
    }
    
    /**
     * Preloads all common fonts on application startup to avoid UI stuttering
     */
    fun preloadCommonFonts(context: Context) {
        val job = SupervisorJob()
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO + job).launch {
            try {
                // List of fonts to preload - each font is loaded individually to ensure one bad font doesn't
                // prevent loading the others
                val fontIds = listOf(
                    com.example.gotouchthatgrass_3.R.font.poppins_regular,
                    com.example.gotouchthatgrass_3.R.font.poppins_bold,
                    com.example.gotouchthatgrass_3.R.font.poppins_medium,
                    com.example.gotouchthatgrass_3.R.font.poppins_light,
                    com.example.gotouchthatgrass_3.R.font.montserrat_regular_font
                )
                
                var successCount = 0
                
                // Try to load each font individually so one failure doesn't prevent the others
                fontIds.forEach { fontId ->
                    try {
                        if (!typefaceCache.containsKey(fontId)) {
                            // Load the font
                            val typeface = ResourcesCompat.getFont(context, fontId)
                            
                            // Only cache if successfully loaded
                            if (typeface != null) {
                                typefaceCache[fontId] = typeface
                                successCount++
                            }
                        } else {
                            // Already cached
                            successCount++
                        }
                    } catch (e: Exception) {
                        // Log but continue with other fonts
                        Log.e("FontLoader", "Error loading font resource ID: $fontId", e)
                    }
                }
                
                Log.d("FontLoader", "Preloaded $successCount/${fontIds.size} fonts")
            } catch (e: Exception) {
                Log.e("FontLoader", "Error in font preloading process", e)
            }
        }
    }
}
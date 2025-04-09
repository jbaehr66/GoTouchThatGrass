package com.example.gotouchthatgrass_3.ui.screentime

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.gotouchthatgrass_3.R
import com.example.gotouchthatgrass_3.databinding.FragmentScreenTimeBinding
import com.example.gotouchthatgrass_3.ui.theme.FontCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Helper class for ScreenTimeFragment to apply fonts programmatically
 * with enhanced error handling and fallbacks
 */
object ScreenTimeHelper {
    
    /**
     * Apply fonts to all views in the ScreenTimeFragment using FontCache
     * with comprehensive error handling
     */
    fun applyFonts(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        binding: FragmentScreenTimeBinding
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("ScreenTimeHelper", "Starting font application")
                
                // Map of views to their respective font resources
                val fontMappings = HashMap<TextView, Int>().apply {
                    put(binding.currentDateText, R.font.poppins_medium)
                    put(binding.screenTimeLabel, R.font.poppins_light)
                    put(binding.screenTimeValue, R.font.poppins_bold)
                    put(binding.comparisonText, R.font.poppins_regular)
                    put(binding.improvementText, R.font.poppins_light)
                    put(binding.appUsageTitle, R.font.poppins_medium)
                    put(binding.noDataText, R.font.poppins_regular)
                }
                
                // Apply each font with fallback
                fontMappings.forEach { (view, fontResId) ->
                    try {
                        // Always get a typeface - either from cache or system default
                        val typeface = FontCache.getFont(context, fontResId) ?: Typeface.DEFAULT
                        
                        withContext(Dispatchers.Main) {
                            try {
                                view.typeface = typeface
                                Log.d("ScreenTimeHelper", "Applied font to view")
                            } catch (e: Exception) {
                                Log.e("ScreenTimeHelper", "Error applying typeface to view", e)
                                
                                // Final fallback attempt
                                try {
                                    view.typeface = Typeface.DEFAULT
                                } catch (e2: Exception) {
                                    Log.e("ScreenTimeHelper", "Even system font fallback failed", e2)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ScreenTimeHelper", "Error getting font $fontResId", e)
                    }
                }
                Log.d("ScreenTimeHelper", "Font application complete")
            } catch (e: Exception) {
                Log.e("ScreenTimeHelper", "Unexpected error in applyFonts", e)
            }
        }
    }
    
    /**
     * Apply a font safely to any TextView with full error handling
     */
    fun safeApplyFont(context: Context, textView: TextView?, fontResId: Int) {
        if (textView == null) return
        
        try {
            // Get typeface with system font fallback
            val typeface = try {
                FontCache.getFont(context, fontResId) ?: Typeface.DEFAULT
            } catch (e: Exception) {
                Log.e("ScreenTimeHelper", "Error loading font $fontResId, falling back to DEFAULT", e)
                Typeface.DEFAULT
            }
            
            // Apply typeface
            try {
                textView.typeface = typeface
            } catch (e: Exception) {
                Log.e("ScreenTimeHelper", "Error applying font to TextView", e)
                try {
                    textView.typeface = Typeface.DEFAULT
                } catch (e2: Exception) {
                    Log.e("ScreenTimeHelper", "Even system font fallback failed", e2)
                }
            }
        } catch (e: Exception) {
            Log.e("ScreenTimeHelper", "Unexpected error in safeApplyFont", e)
        }
    }
}
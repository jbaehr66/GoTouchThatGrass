package com.example.gotouchthatgrass_3.ui.home

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.gotouchthatgrass_3.R
import com.example.gotouchthatgrass_3.databinding.FragmentHomeBinding
import com.example.gotouchthatgrass_3.ui.theme.FontCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Helper class for HomeFragment to apply fonts programmatically
 * with enhanced error handling and fallbacks
 */
object HomeFragmentHelper {
    
    /**
     * Apply fonts to all views in the HomeFragment using FontCache
     * with comprehensive error handling
     */
    fun applyFonts(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        binding: FragmentHomeBinding
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("HomeFragmentHelper", "Starting font application")
                
                // Map of views to their respective font resources
                val fontMappings = HashMap<TextView, Int>().apply {
                    put(binding.statusText, R.font.poppins_regular)
                    put(binding.lastChallengeText, R.font.poppins_light)
                    put(binding.streakLabel, R.font.poppins_medium)
                    put(binding.streakText, R.font.poppins_bold)
                    put(binding.blockedAppsLabel, R.font.poppins_medium)
                    put(binding.blockedAppsCount, R.font.poppins_bold)
                    put(binding.blockedAppsNames, R.font.poppins_light)
                    put(binding.noBlockedAppsText, R.font.poppins_light)
                    put(binding.startChallengeButton, R.font.poppins_medium)
                }
                
                // Apply each font with fallback
                fontMappings.forEach { (view, fontResId) ->
                    try {
                        // Always get a typeface - either from cache or system default
                        val typeface = FontCache.getFont(context, fontResId) ?: Typeface.DEFAULT
                        
                        withContext(Dispatchers.Main) {
                            try {
                                view.typeface = typeface
                                Log.d("HomeFragmentHelper", "Applied font to ${getViewName(view)}")
                            } catch (e: Exception) {
                                Log.e("HomeFragmentHelper", "Error applying typeface to view", e)
                                
                                // Final fallback attempt
                                try {
                                    view.typeface = Typeface.DEFAULT
                                } catch (e2: Exception) {
                                    Log.e("HomeFragmentHelper", "Even system font fallback failed", e2)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("HomeFragmentHelper", "Error getting font $fontResId", e)
                    }
                }
                Log.d("HomeFragmentHelper", "Font application complete")
            } catch (e: Exception) {
                Log.e("HomeFragmentHelper", "Unexpected error in applyFonts", e)
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
                Log.e("HomeFragmentHelper", "Error loading font $fontResId, falling back to DEFAULT", e)
                Typeface.DEFAULT
            }
            
            // Apply typeface
            try {
                textView.typeface = typeface
            } catch (e: Exception) {
                Log.e("HomeFragmentHelper", "Error applying font to TextView", e)
                try {
                    textView.typeface = Typeface.DEFAULT
                } catch (e2: Exception) {
                    Log.e("HomeFragmentHelper", "Even system font fallback failed", e2)
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragmentHelper", "Unexpected error in safeApplyFont", e)
        }
    }
    
    /**
     * Get a text description of a view for logging purposes
     */
    private fun getViewName(view: TextView): String {
        return when (view.id) {
            R.id.statusText -> "statusText"
            R.id.lastChallengeText -> "lastChallengeText"
            R.id.streakLabel -> "streakLabel"
            R.id.streakText -> "streakText"
            R.id.blockedAppsLabel -> "blockedAppsLabel"
            R.id.blockedAppsCount -> "blockedAppsCount"
            R.id.blockedAppsNames -> "blockedAppsNames"
            R.id.noBlockedAppsText -> "noBlockedAppsText"
            R.id.startChallengeButton -> "startChallengeButton"
            else -> "unknown(${view.id})"
        }
    }
}
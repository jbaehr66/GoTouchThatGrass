package com.example.gotouchthatgrass_3.ui.stats

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.gotouchthatgrass_3.R
import com.example.gotouchthatgrass_3.databinding.FragmentStatsBinding
import com.example.gotouchthatgrass_3.ui.theme.FontCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Helper class for StatsFragment to apply fonts programmatically
 * with enhanced error handling and fallbacks
 */
object StatsFragmentHelper {
    
    /**
     * Apply fonts to all views in the StatsFragment using FontCache
     * with comprehensive error handling
     */
    fun applyFonts(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        binding: FragmentStatsBinding
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("StatsFragmentHelper", "Starting font application")
                
                // Map of views to their respective font resources
                val fontMappings = HashMap<TextView, Int>().apply {
                    put(binding.historyTitle, R.font.poppins_medium)
                    put(binding.streakCountText, R.font.poppins_bold)
                    put(binding.lastChallengeText, R.font.poppins_light)
                    put(binding.totalChallengesText, R.font.poppins_bold)
                    put(binding.successRateText, R.font.poppins_bold)
                    put(binding.noDataText, R.font.poppins_light)
                    
                    // Add the labels in cardviews (need to get them via parent view)
                    try {
                        // Get font resource IDs outside of lambdas to help Kotlin with type inference
                        val mediumFontId = R.font.poppins_medium
                        
                        // The CardViews have LinearLayouts with two TextViews each
                        // First TextView is the label, second is the value
                        val streakCard = binding.streakCard
                        if (streakCard != null) {
                            val linearLayout = streakCard.getChildAt(0) as? LinearLayout
                            if (linearLayout != null) {
                                // The first child is the label TextView
                                val labelView = linearLayout.getChildAt(0) as? TextView
                                if (labelView != null) {
                                    put(labelView, mediumFontId)
                                    Log.d("StatsFragmentHelper", "Found streak label")
                                }
                            }
                        }
                        
                        val totalChallengesCard = binding.totalChallengesCard
                        if (totalChallengesCard != null) {
                            val linearLayout = totalChallengesCard.getChildAt(0) as? LinearLayout
                            if (linearLayout != null) {
                                val labelView = linearLayout.getChildAt(0) as? TextView
                                if (labelView != null) {
                                    put(labelView, mediumFontId)
                                    Log.d("StatsFragmentHelper", "Found total challenges label")
                                }
                            }
                        }
                        
                        val successRateCard = binding.successRateCard
                        if (successRateCard != null) {
                            val linearLayout = successRateCard.getChildAt(0) as? LinearLayout
                            if (linearLayout != null) {
                                val labelView = linearLayout.getChildAt(0) as? TextView
                                if (labelView != null) {
                                    put(labelView, mediumFontId)
                                    Log.d("StatsFragmentHelper", "Found success rate label")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("StatsFragmentHelper", "Error accessing card labels", e)
                    }
                }
                
                // Apply each font with fallback
                fontMappings.forEach { (view, fontResId) ->
                    try {
                        // Always get a typeface - either from cache or system default
                        val typeface = FontCache.getFont(context, fontResId) ?: Typeface.DEFAULT
                        
                        withContext(Dispatchers.Main) {
                            try {
                                view.typeface = typeface
                                Log.d("StatsFragmentHelper", "Applied font to ${getViewName(view)}")
                            } catch (e: Exception) {
                                Log.e("StatsFragmentHelper", "Error applying typeface to view", e)
                                
                                // Final fallback attempt
                                try {
                                    view.typeface = Typeface.DEFAULT
                                } catch (e2: Exception) {
                                    Log.e("StatsFragmentHelper", "Even system font fallback failed", e2)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("StatsFragmentHelper", "Error getting font $fontResId", e)
                    }
                }
                Log.d("StatsFragmentHelper", "Font application complete")
            } catch (e: Exception) {
                Log.e("StatsFragmentHelper", "Unexpected error in applyFonts", e)
            }
        }
    }
    
    /**
     * Apply a font safely to any TextView with full error handling
     * For use in RecyclerView items
     */
    fun safeApplyFont(
        context: Context, 
        lifecycleScope: LifecycleCoroutineScope,
        textView: TextView?, 
        fontResId: Int
    ) {
        if (textView == null) return
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Get typeface with system font fallback
                val typeface = try {
                    FontCache.getFont(context, fontResId) ?: Typeface.DEFAULT
                } catch (e: Exception) {
                    Log.e("StatsFragmentHelper", "Error loading font $fontResId, falling back to DEFAULT", e)
                    Typeface.DEFAULT
                }
                
                // Apply typeface on main thread
                withContext(Dispatchers.Main) {
                    try {
                        // Check if view is still valid before applying font
                        if (textView.isAttachedToWindow) {
                            textView.typeface = typeface
                        }
                    } catch (e: Exception) {
                        Log.e("StatsFragmentHelper", "Error applying font to TextView", e)
                        try {
                            textView.typeface = Typeface.DEFAULT
                        } catch (e2: Exception) {
                            Log.e("StatsFragmentHelper", "Even system font fallback failed", e2)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("StatsFragmentHelper", "Unexpected error in safeApplyFont", e)
            }
        }
    }
    
    /**
     * Get a text description of a view for logging purposes
     */
    private fun getViewName(view: TextView): String {
        return when (view.id) {
            R.id.historyTitle -> "historyTitle"
            R.id.streakCountText -> "streakCountText"
            R.id.lastChallengeText -> "lastChallengeText"
            R.id.totalChallengesText -> "totalChallengesText"
            R.id.successRateText -> "successRateText"
            R.id.noDataText -> "noDataText"
            android.R.id.text1 -> "cardLabel"
            else -> "unknown(${view.id})"
        }
    }
}
package com.example.gotouchthatgrass_3.ui.blocked

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.gotouchthatgrass_3.R
import com.example.gotouchthatgrass_3.databinding.FragmentBlockedAppsBinding
import com.example.gotouchthatgrass_3.ui.theme.FontCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Helper class for BlockedAppsFragment to apply fonts programmatically
 * with enhanced error handling and fallbacks
 */
object BlockedAppsHelper {
    
    /**
     * Apply fonts to all views in the BlockedAppsFragment using FontCache
     */
    fun applyFonts(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        binding: FragmentBlockedAppsBinding
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Map of views to their respective font resources
                val fontMappings = HashMap<TextView, Int>().apply {
                    put(binding.blockedAppsTitle, R.font.poppins_medium)
                    put(binding.noBlockedAppsText, R.font.poppins_regular)
                    put(binding.btnUnblockAll, R.font.poppins_medium)
                    put(binding.availableAppsTitle, R.font.poppins_medium)
                }
                
                // Apply each font with fallback
                fontMappings.forEach { (view, fontResId) ->
                    try {
                        // Attempt to get the font from cache
                        val typeface = FontCache.getFont(context, fontResId) ?: Typeface.DEFAULT
                        
                        withContext(Dispatchers.Main) {
                            try {
                                view.typeface = typeface
                            } catch (e: Exception) {
                                Log.e("BlockedAppsHelper", "Error applying typeface to view", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("BlockedAppsHelper", "Error loading font $fontResId", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("BlockedAppsHelper", "Error in applyFonts", e)
            }
        }
    }
    
    /**
     * Apply fonts to views in recycler view adapters with enhanced error handling
     */
    fun applyFontToView(context: Context, view: View, fontResId: Int) {
        try {
            if (view is TextView || view is Button) {
                // Get typeface with fallback to system default
                val typeface = try {
                    FontCache.getFont(context, fontResId) ?: Typeface.DEFAULT
                } catch (e: Exception) {
                    Log.e("BlockedAppsHelper", "Error getting font $fontResId, using DEFAULT", e)
                    Typeface.DEFAULT
                }
                
                // Apply typeface based on view type
                try {
                    when (view) {
                        is TextView -> view.typeface = typeface
                        is Button -> view.typeface = typeface
                    }
                } catch (e: Exception) {
                    Log.e("BlockedAppsHelper", "Error applying typeface to view", e)
                }
            }
        } catch (e: Exception) {
            Log.e("BlockedAppsHelper", "Unexpected error in applyFontToView", e)
        }
    }
    
    /**
     * Apply a font safely to any TextView with full error handling
     */
    fun safeApplyFont(context: Context, textView: TextView?, fontResId: Int) {
        if (textView == null) return
        
        try {
            val typeface = FontCache.getFont(context, fontResId) ?: Typeface.DEFAULT
            textView.typeface = typeface
        } catch (e: Exception) {
            Log.e("BlockedAppsHelper", "Error applying font to TextView", e)
            try {
                textView.typeface = Typeface.DEFAULT
            } catch (e2: Exception) {
                Log.e("BlockedAppsHelper", "Even fallback font failed", e2)
            }
        }
    }
}
package com.example.gotouchthatgrass_3.ui.theme

import android.content.Context
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.gotouchthatgrass_3.R

/**
 * Helper utility for applying fonts to UI components
 * using our FontLoader to ensure fonts are loaded off the UI thread
 */
object FontHelper {
    
    /**
     * Apply the appropriate fonts to all TextViews in the HomeFragment
     */
    fun applyFontsToHomeFragment(fragment: Fragment, binding: Any) {
        val context = fragment.requireContext()
        val lifecycleScope = fragment.viewLifecycleOwner.lifecycleScope
        
        // Extract all TextViews from binding using reflection
        binding.javaClass.declaredFields.forEach { field ->
            field.isAccessible = true
            val view = field.get(binding)
            
            when {
                view is TextView -> {
                    when (field.name) {
                        "statusText" -> 
                            FontLoader.loadFontAsync(lifecycleScope, context, view, R.font.poppins_regular)
                        "lastChallengeText" -> 
                            FontLoader.loadFontAsync(lifecycleScope, context, view, R.font.poppins_light)
                        "streakLabel" -> 
                            FontLoader.loadFontAsync(lifecycleScope, context, view, R.font.poppins_medium)
                        "streakText" -> 
                            FontLoader.loadFontAsync(lifecycleScope, context, view, R.font.poppins_bold)
                        "blockedAppsLabel" -> 
                            FontLoader.loadFontAsync(lifecycleScope, context, view, R.font.poppins_medium)
                        "blockedAppsCount" -> 
                            FontLoader.loadFontAsync(lifecycleScope, context, view, R.font.poppins_bold) 
                        "blockedAppsNames", "noBlockedAppsText" -> 
                            FontLoader.loadFontAsync(lifecycleScope, context, view, R.font.poppins_light)
                    }
                }
                view is Button -> {
                    FontLoader.loadFontAsync(lifecycleScope, context, view, R.font.poppins_medium)
                }
            }
        }
    }
    
    /**
     * Apply the appropriate fonts to all TextViews in the BlockedAppsFragment
     */
    fun applyFontsToBlockedAppsFragment(fragment: Fragment, binding: Any) {
        val context = fragment.requireContext()
        val lifecycleScope = fragment.viewLifecycleOwner.lifecycleScope
        
        // Extract all TextViews from binding using reflection
        binding.javaClass.declaredFields.forEach { field ->
            field.isAccessible = true
            val view = field.get(binding)
            
            when {
                view is TextView -> {
                    when (field.name) {
                        "blockedAppsTitle", "availableAppsTitle" -> 
                            FontLoader.loadFontAsync(lifecycleScope, context, view, R.font.poppins_medium)
                        "noBlockedAppsText" -> 
                            FontLoader.loadFontAsync(lifecycleScope, context, view, R.font.poppins_light)
                    }
                }
                view is Button -> {
                    FontLoader.loadFontAsync(lifecycleScope, context, view, R.font.poppins_medium)
                }
            }
        }
    }
    
    /**
     * Apply the appropriate fonts to all TextViews in the StatsFragment
     */
    fun applyFontsToStatsFragment(fragment: Fragment, binding: Any) {
        val context = fragment.requireContext()
        val lifecycleScope = fragment.viewLifecycleOwner.lifecycleScope
        
        // Extract all TextViews from binding using reflection
        binding.javaClass.declaredFields.forEach { field ->
            field.isAccessible = true
            val view = field.get(binding)
            
            when {
                view is TextView -> {
                    when (field.name) {
                        "statsTitle", "challengeHistoryTitle" -> 
                            FontLoader.loadFontAsync(lifecycleScope, context, view, R.font.poppins_medium)
                        "streakCountText", "totalChallengesText" -> 
                            FontLoader.loadFontAsync(lifecycleScope, context, view, R.font.poppins_bold)
                        "streakLabelText", "totalChallengesLabel", "noChallengesText" -> 
                            FontLoader.loadFontAsync(lifecycleScope, context, view, R.font.poppins_light)
                    }
                }
            }
        }
    }
}
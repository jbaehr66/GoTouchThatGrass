package com.example.gotouchthatgrass_3.ui.theme

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.gotouchthatgrass_3.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Custom LayoutInflater Factory that programmatically applies fonts
 * to avoid StrictMode violations from XML fontFamily attributes
 */
class FontLayoutInflaterFactory(private val context: Context) : LayoutInflater.Factory2 {
    
    override fun onCreateView(
        parent: View?, 
        name: String, 
        context: Context, 
        attrs: android.util.AttributeSet
    ): View? {
        // Let the default factory create the view
        val view = null // Returning null lets the default factory handle creation
        
        // Apply fonts to newly created view
        if (view != null) {
            applyFontToView(context, view)
        }
        
        return view
    }
    
    override fun onCreateView(name: String, context: Context, attrs: android.util.AttributeSet): View? {
        return onCreateView(null, name, context, attrs)
    }
    
    private fun applyFontToView(context: Context, view: View) {
        when (view) {
            is TextView -> applyFontToTextView(context, view)
            is Button -> applyFontToButton(context, view)
            is ViewGroup -> {
                // Process children if this is a ViewGroup
                val childCount = view.childCount
                for (i in 0 until childCount) {
                    applyFontToView(context, view.getChildAt(i))
                }
            }
        }
    }
    
    private fun applyFontToTextView(context: Context, textView: TextView) {
        val fontResId = R.font.poppins_regular
        val typeface = FontCache.getFont(context, fontResId)
        if (typeface != null) {
            textView.typeface = typeface
        }
    }
    
    private fun applyFontToButton(context: Context, button: Button) {
        val fontResId = R.font.poppins_medium
        val typeface = FontCache.getFont(context, fontResId)
        if (typeface != null) {
            button.typeface = typeface
        }
    }
}

/**
 * Helper to apply fonts to views generated with styles that had fontFamily attributes
 * This replaces the XML android:fontFamily references with programmatic font application
 */
object StyleFontApplier {
    
    /**
     * Apply fonts to all TextViews and Buttons in a view hierarchy
     * This should be called when a new layout is inflated
     */
    fun applyFontsToViewHierarchy(context: Context, rootView: View) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("StyleFontApplier", "Applying fonts to view hierarchy")
                applyFontsRecursively(context, rootView)
                Log.d("StyleFontApplier", "Completed applying fonts to view hierarchy")
            } catch (e: Exception) {
                Log.e("StyleFontApplier", "Error applying fonts", e)
            }
        }
    }
    
    /**
     * Recursively traverse view hierarchy and apply fonts
     */
    private fun applyFontsRecursively(context: Context, view: View) {
        try {
            // Apply font based on view's style
            when (view) {
                is TextView -> applyFontToTextView(context, view)
                is Button -> applyFontToButton(context, view)
            }
            
            // If this is a ViewGroup, process all children
            if (view is ViewGroup) {
                val childCount = view.childCount
                for (i in 0 until childCount) {
                    val child = view.getChildAt(i)
                    applyFontsRecursively(context, child)
                }
            }
        } catch (e: Exception) {
            Log.e("StyleFontApplier", "Error in applyFontsRecursively", e)
        }
    }
    
    /**
     * Apply appropriate font to a TextView based on its style
     */
    private fun applyFontToTextView(context: Context, textView: TextView) {
        try {
            // Determine which font to apply based on text appearance
            val fontResId = when {
                // Handle known TextAppearance styles from styles.xml
                textView.textSize >= 24f -> R.font.poppins_bold        // Title
                textView.textSize >= 20f -> R.font.poppins_medium      // Headline
                textView.textSize >= 16f -> R.font.poppins_regular     // Subtitle
                textView.textSize >= 14f -> R.font.poppins_light       // Body
                textView.textSize <= 13f -> R.font.poppins_light       // Caption/Small
                else -> R.font.poppins_regular                         // Default
            }
            
            // Apply the font safely
            val typeface = FontCache.getFont(context, fontResId)
            if (typeface != null) {
                textView.post {
                    textView.typeface = typeface
                }
            }
        } catch (e: Exception) {
            Log.e("StyleFontApplier", "Error applying font to TextView", e)
        }
    }
    
    /**
     * Apply appropriate font to a Button based on its style
     */
    private fun applyFontToButton(context: Context, button: Button) {
        try {
            // Most buttons use medium font weight
            val fontResId = R.font.poppins_medium
            
            // Apply the font safely
            val typeface = FontCache.getFont(context, fontResId)
            if (typeface != null) {
                button.post {
                    button.typeface = typeface
                }
            }
        } catch (e: Exception) {
            Log.e("StyleFontApplier", "Error applying font to Button", e)
        }
    }
}
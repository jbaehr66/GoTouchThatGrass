package com.example.gotouchthatgrass_3.util

import android.os.SystemClock
import android.util.Log
import android.view.View

/**
 * Utility functions for handling view clicks, including debouncing
 */
object ClickUtils {
    // Tracks the last click timestamp for each view
    private val lastClickTimestamps = HashMap<Int, Long>()
    
    // Default debounce interval in milliseconds
    private const val DEFAULT_DEBOUNCE_INTERVAL = 500L
    
    /**
     * Sets a debounced click listener to a view, preventing rapid multiple clicks
     *
     * @param view The view to set the click listener on
     * @param debounceTime The debounce time in milliseconds (default 500ms)
     * @param clickAction The action to perform on click
     */
    fun setDebouncedClickListener(
        view: View,
        debounceTime: Long = DEFAULT_DEBOUNCE_INTERVAL,
        clickAction: (View) -> Unit
    ) {
        view.setOnClickListener {
            if (isDoubleClick(it, debounceTime)) {
                Log.d("ClickUtils", "Ignoring rapid click on view ${view.id}")
                return@setOnClickListener
            }
            
            clickAction(it)
        }
    }
    
    /**
     * Checks if a click is too soon after a previous click
     *
     * @param view The view that was clicked
     * @param debounceTime The minimum time between clicks in milliseconds
     * @return True if this click should be ignored (double click), false otherwise
     */
    private fun isDoubleClick(view: View, debounceTime: Long): Boolean {
        val currentClickTime = SystemClock.elapsedRealtime()
        val lastClickTime = lastClickTimestamps[view.id] ?: 0L
        
        // Update the last click time for this view
        lastClickTimestamps[view.id] = currentClickTime
        
        // If the time since the last click is less than the debounce time, ignore this click
        return currentClickTime - lastClickTime < debounceTime
    }
}
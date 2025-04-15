package com.example.gotouchthatgrass_3.models

import android.graphics.drawable.Drawable
import com.example.gotouchthatgrass_3.models.AppCategory

/**
 * Data class representing an app item in the UI
 */
data class AppItem(
    val packageName: String,
    val label: CharSequence,
    val icon: Drawable,
    val category: AppCategory = AppCategory.OTHER
)

/**
 * Data class representing a blocked app item in the UI
 */
data class BlockedAppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val blockTime: Long = System.currentTimeMillis() // timestamp when the app was blocked
) {
    override fun toString(): String {
        return appName
    }
}
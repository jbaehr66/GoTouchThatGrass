package com.example.gotouchthatgrass_3.ui.screentime

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gotouchthatgrass_3.R
import com.example.gotouchthatgrass_3.ui.theme.FontCache
import java.util.concurrent.TimeUnit

class AppUsageAdapter(private val context: Context) : 
    ListAdapter<ScreenTimeFragment.AppUsageInfo, AppUsageAdapter.ViewHolder>(AppUsageDiffCallback()) {

    // Map of app categories to colors
    private val categoryColors = mapOf(
        "Social" to Color.parseColor("#2196F3"),  // Blue
        "Games" to Color.parseColor("#FF9800"),  // Orange
        "Entertainment" to Color.parseColor("#9C27B0"),  // Purple
        "Productivity" to Color.parseColor("#4CAF50"),  // Green
        "Finance" to Color.parseColor("#607D8B"),  // Blue Grey
        "Other" to Color.parseColor("#757575")   // Grey
    )
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_usage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appUsage = getItem(position)
        holder.bind(appUsage)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appIconLetter: TextView = itemView.findViewById(R.id.appIconLetter)
        private val appIconContainer: CardView = itemView.findViewById(R.id.appIconContainer)
        private val appNameText: TextView = itemView.findViewById(R.id.appNameText)
        private val appCategoryText: TextView = itemView.findViewById(R.id.appCategoryText)
        private val appUsageTimeText: TextView = itemView.findViewById(R.id.appUsageTimeText)
        
        // Apply fonts to views
        init {
            try {
                val poppinsRegular = FontCache.getFont(context, R.font.poppins_regular)
                val poppinsLight = FontCache.getFont(context, R.font.poppins_light)
                val poppinsMedium = FontCache.getFont(context, R.font.poppins_medium)
                
                appNameText.typeface = poppinsRegular
                appCategoryText.typeface = poppinsLight
                appUsageTimeText.typeface = poppinsMedium
                appIconLetter.typeface = poppinsRegular
            } catch (e: Exception) {
                Log.e("AppUsageAdapter", "Error applying fonts", e)
            }
        }
        
        fun bind(appUsage: ScreenTimeFragment.AppUsageInfo) {
            // Set app name
            appNameText.text = appUsage.appName.toLowerCase()
            
            // Set first letter of app name as icon
            val firstLetter = if (appUsage.appName.isNotEmpty()) {
                appUsage.appName.first().toString().toLowerCase()
            } else {
                "?"
            }
            appIconLetter.text = firstLetter
            
            // Set app category
            appCategoryText.text = appUsage.category
            
            // Set category color
            val categoryColor = categoryColors[appUsage.category] ?: categoryColors["Other"]!!
            appIconContainer.setCardBackgroundColor(categoryColor)
            appCategoryText.setTextColor(categoryColor)
            
            // Format usage time
            val hours = TimeUnit.MILLISECONDS.toHours(appUsage.usageTimeMs)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(appUsage.usageTimeMs) % 60
            
            appUsageTimeText.text = when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                else -> "<1m"
            }
            
            // Try to load app icon (in a real app, you would use Glide or similar library)
            try {
                val packageManager = context.packageManager
                val appIcon = packageManager.getApplicationIcon(appUsage.packageName)
                // In a real implementation, you would set this icon to an ImageView
                // For now, we'll just use the letter as a placeholder
            } catch (e: PackageManager.NameNotFoundException) {
                // App not found, keep using the letter
                Log.d("AppUsageAdapter", "App icon not found for ${appUsage.packageName}")
            }
        }
    }
}

class AppUsageDiffCallback : DiffUtil.ItemCallback<ScreenTimeFragment.AppUsageInfo>() {
    override fun areItemsTheSame(
        oldItem: ScreenTimeFragment.AppUsageInfo, 
        newItem: ScreenTimeFragment.AppUsageInfo
    ): Boolean {
        return oldItem.packageName == newItem.packageName
    }

    override fun areContentsTheSame(
        oldItem: ScreenTimeFragment.AppUsageInfo, 
        newItem: ScreenTimeFragment.AppUsageInfo
    ): Boolean {
        return oldItem == newItem
    }
}
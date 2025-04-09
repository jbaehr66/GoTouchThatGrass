package com.example.gotouchthatgrass_3.ui.blocked

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gotouchthatgrass_3.R
import com.example.gotouchthatgrass_3.databinding.ItemAppBinding
import com.example.gotouchthatgrass_3.util.ClickUtils

class AppListAdapter(private val onBlockClick: (AppItem) -> Unit) :
    ListAdapter<AppItem, AppListAdapter.ViewHolder>(AppDiffCallback()) {

    private var fullList = ArrayList<AppItem>()
    private var lastQuery = ""

    override fun submitList(list: List<AppItem>?) {
        if (list != null) {
            fullList = ArrayList(list)
            android.util.Log.d("AppListAdapter", "Saved ${fullList.size} items to full list")
            
            // Apply any existing filter
            if (lastQuery.isNotEmpty()) {
                android.util.Log.d("AppListAdapter", "Reapplying last query: $lastQuery")
                applyFilter(lastQuery)
            } else {
                super.submitList(list)
            }
        } else {
            super.submitList(list)
        }
    }

    fun filter(query: String) {
        lastQuery = query
        applyFilter(query)
    }
    
    private fun applyFilter(query: String) {
        android.util.Log.d("AppListAdapter", "Filtering with query: '$query', fullList size: ${fullList.size}")
        
        // Guard against empty list
        if (fullList.isEmpty()) {
            android.util.Log.w("AppListAdapter", "Full list is empty, nothing to filter")
            super.submitList(emptyList())
            return
        }
        
        val filteredList = if (query.isEmpty()) {
            fullList
        } else {
            val lowercaseQuery = query.lowercase()
            fullList.filter { app ->
                val appNameLower = app.label.toString().lowercase()
                val contains = appNameLower.contains(lowercaseQuery)
                
                // Log some example matches for debugging
                if (contains || fullList.indexOf(app) < 3) {
                    android.util.Log.d("AppListAdapter", "App: ${app.label}, query: $lowercaseQuery, matches: $contains")
                }
                
                contains
            }
        }
        
        android.util.Log.d("AppListAdapter", "Filter results: ${filteredList.size} items")
        
        // Special case for no results - show message in fragment
        if (filteredList.isEmpty() && query.isNotEmpty()) {
            android.util.Log.w("AppListAdapter", "No results for query: $query")
        }
        
        // Clone the list to force DiffUtil to recalculate
        val newList = ArrayList(filteredList)
        
        // Use post to ensure UI updates smoothly
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            super.submitList(null)  // First clear the list
            super.submitList(newList)  // Then update with new data
        }
    }

    inner class ViewHolder(val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = getItem(position)
        val context = holder.itemView.context
        
        with(holder.binding) {
            // Set text and drawable
            appName.text = app.label
            appIcon.setImageDrawable(app.icon)
            
            // Apply fonts from cache
            BlockedAppsHelper.applyFontToView(context, appName, R.font.poppins_regular)
            BlockedAppsHelper.applyFontToView(context, blockButton, R.font.poppins_medium)
            
            // Set debounced click listener to prevent rapid multiple clicks
            ClickUtils.setDebouncedClickListener(blockButton, 1000) {
                Log.d("AppListAdapter", "Block button clicked for ${app.label}")
                onBlockClick(app)
            }
        }
    }
}

class AppDiffCallback : DiffUtil.ItemCallback<AppItem>() {
    override fun areItemsTheSame(oldItem: AppItem, newItem: AppItem): Boolean {
        // Items are the same if they represent the same app package
        return oldItem.packageName == newItem.packageName
    }

    override fun areContentsTheSame(oldItem: AppItem, newItem: AppItem): Boolean {
        // Compare all fields to determine if anything has changed
        // (Excluding drawable which doesn't implement equals)
        return oldItem.packageName == newItem.packageName && 
               oldItem.label.toString() == newItem.label.toString()
    }
}
package com.example.gotouchthatgrass_3.ui.blocked

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gotouchthatgrass_3.R
import com.example.gotouchthatgrass_3.databinding.ItemBlockedAppBinding
import com.example.gotouchthatgrass_3.models.BlockedAppItem
import com.example.gotouchthatgrass_3.util.ClickUtils
import java.util.concurrent.TimeUnit

class BlockedAppAdapter(private val onUnblockClick: (BlockedAppItem) -> Unit) :
    ListAdapter<BlockedAppItem, BlockedAppAdapter.ViewHolder>(BlockedAppDiffCallback()) {

    inner class ViewHolder(val binding: ItemBlockedAppBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBlockedAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val blockedApp = getItem(position)
        val context = holder.itemView.context
        
        with(holder.binding) {
            // Set text and drawable
            appName.text = blockedApp.appName
            appIcon.setImageDrawable(blockedApp.icon)
            
            // Set block time display
            val blockTimeText = formatBlockTime(blockedApp.blockTime)
            blockTime.text = context.getString(R.string.blocked_time, blockTimeText)
            
            // Set credit cost for unblocking
            val cost = calculateUnblockCost(blockedApp.blockTime)
            creditCost.text = context.getString(R.string.credits_cost, cost)
            
            // Apply fonts from cache
            BlockedAppsHelper.applyFontToView(context, appName, R.font.poppins_regular)
            BlockedAppsHelper.applyFontToView(context, blockTime, R.font.poppins_regular)
            BlockedAppsHelper.applyFontToView(context, creditCost, R.font.poppins_medium)
            BlockedAppsHelper.applyFontToView(context, unblockButton, R.font.poppins_medium)
            
            // Set debounced click listener to prevent rapid multiple clicks
            ClickUtils.setDebouncedClickListener(unblockButton, 1000) {
                Log.d("BlockedAppAdapter", "Unblock button clicked for ${blockedApp.appName}")
                onUnblockClick(blockedApp)
            }
        }
    }
    
    /**
     * Format the block time into a human-readable string
     */
    private fun formatBlockTime(blockTimeMillis: Long): String {
        val now = System.currentTimeMillis()
        val durationMillis = now - blockTimeMillis
        
        return when {
            durationMillis < TimeUnit.MINUTES.toMillis(1) -> "just now"
            durationMillis < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
                "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
            }
            durationMillis < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
                "$hours ${if (hours == 1L) "hour" else "hours"} ago"
            }
            durationMillis < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(durationMillis)
                "$days ${if (days == 1L) "day" else "days"} ago"
            }
            else -> {
                val days = TimeUnit.MILLISECONDS.toDays(durationMillis)
                "$days days ago"
            }
        }
    }
    
    /**
     * Calculate the cost to unblock an app based on block duration
     * This should match the calculation in CreditsViewModel
     */
    private fun calculateUnblockCost(blockTimeMillis: Long): Int {
        val now = System.currentTimeMillis()
        val blockDurationHours = (now - blockTimeMillis) / (1000 * 60 * 60)
        
        // Base cost is 1 credit, plus 1 credit for each day it's been blocked (max 5)
        return (1 + (blockDurationHours / 24).coerceAtMost(4)).toInt()
    }
}

class BlockedAppDiffCallback : DiffUtil.ItemCallback<BlockedAppItem>() {
    override fun areItemsTheSame(oldItem: BlockedAppItem, newItem: BlockedAppItem): Boolean {
        return oldItem.packageName == newItem.packageName
    }

    override fun areContentsTheSame(oldItem: BlockedAppItem, newItem: BlockedAppItem): Boolean {
        return oldItem == newItem
    }
}
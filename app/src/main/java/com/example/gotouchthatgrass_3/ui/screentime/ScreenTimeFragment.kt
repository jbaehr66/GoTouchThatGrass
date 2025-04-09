package com.example.gotouchthatgrass_3.ui.screentime

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gotouchthatgrass_3.R
import com.example.gotouchthatgrass_3.databinding.FragmentScreenTimeBinding
import com.example.gotouchthatgrass_3.util.AppBlockManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ScreenTimeFragment : Fragment() {

    private var _binding: FragmentScreenTimeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var appUsageAdapter: AppUsageAdapter
    private lateinit var appBlockManager: AppBlockManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScreenTimeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        appBlockManager = AppBlockManager(requireContext())
        
        // Set up the RecyclerView
        setupRecyclerView()
        
        // Load fonts asynchronously using ScreenTimeHelper
        try {
            ScreenTimeHelper.applyFonts(requireContext(), lifecycleScope, binding)
        } catch (e: Exception) {
            Log.e("ScreenTimeFragment", "Error loading fonts", e)
        }
        
        // Set current date
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        binding.currentDateText.text = dateFormat.format(Date())
        
        // Check if we have usage stats permission
        if (appBlockManager.hasUsageStatsPermission()) {
            loadScreenTimeData()
        } else {
            showNoPermissionState()
            // Request permission
            appBlockManager.requestUsageStatsPermission()
        }
    }
    
    private fun setupRecyclerView() {
        appUsageAdapter = AppUsageAdapter(requireContext())
        binding.appUsageRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = appUsageAdapter
        }
    }
    
    private fun loadScreenTimeData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val usageStatsManager = requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                
                // Get today's start time (midnight)
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = calendar.timeInMillis
                
                // Get current time as end time
                val endTime = System.currentTimeMillis()
                
                // Get yesterday's data for comparison
                val yesterdayStart = startTime - TimeUnit.DAYS.toMillis(1)
                val yesterdayEnd = endTime - TimeUnit.DAYS.toMillis(1)
                
                // Query usage stats for today
                val events = usageStatsManager.queryEvents(startTime, endTime)
                
                // Process events to calculate screen time and app usage
                val appUsageMap = calculateAppUsage(events)
                val totalScreenTime = calculateTotalScreenTime(events)
                
                // Query usage stats for yesterday (for comparison)
                val yesterdayEvents = usageStatsManager.queryEvents(yesterdayStart, yesterdayEnd)
                val yesterdayScreenTime = calculateTotalScreenTime(yesterdayEvents)
                
                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    updateScreenTimeUI(totalScreenTime, yesterdayScreenTime)
                    updateAppUsageUI(appUsageMap)
                }
                
            } catch (e: Exception) {
                Log.e("ScreenTimeFragment", "Error loading screen time data", e)
                withContext(Dispatchers.Main) {
                    showErrorState()
                }
            }
        }
    }
    
    private fun calculateAppUsage(events: UsageEvents): Map<String, AppUsageInfo> {
        val appUsageMap = mutableMapOf<String, AppUsageInfo>()
        val event = UsageEvents.Event()
        
        // Track app foreground time
        var currentApp: String? = null
        var lastEventTime: Long = 0
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    currentApp = event.packageName
                    lastEventTime = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (currentApp == event.packageName && lastEventTime > 0) {
                        val usageTime = event.timeStamp - lastEventTime
                        
                        // Get or create app usage info
                        val appInfo = appUsageMap.getOrPut(event.packageName) {
                            val appName = getAppName(event.packageName)
                            val category = getAppCategory(event.packageName)
                            AppUsageInfo(event.packageName, appName, category, 0)
                        }
                        
                        // Add usage time
                        appInfo.usageTimeMs += usageTime
                        appUsageMap[event.packageName] = appInfo
                    }
                    currentApp = null
                }
            }
        }
        
        // Sort by usage time (descending)
        return appUsageMap.toList()
            .sortedByDescending { it.second.usageTimeMs }
            .toMap()
    }
    
    private fun calculateTotalScreenTime(events: UsageEvents): Long {
        var totalScreenTime = 0L
        val event = UsageEvents.Event()
        
        var screenOnTime: Long = 0
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            
            when (event.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    screenOnTime = event.timeStamp
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    if (screenOnTime > 0) {
                        totalScreenTime += event.timeStamp - screenOnTime
                        screenOnTime = 0
                    }
                }
            }
        }
        
        // If screen is still on, add time until now
        if (screenOnTime > 0) {
            totalScreenTime += System.currentTimeMillis() - screenOnTime
        }
        
        return totalScreenTime
    }
    
    private fun updateScreenTimeUI(screenTimeMs: Long, yesterdayScreenTimeMs: Long) {
        // Format screen time as hours and minutes
        val hours = TimeUnit.MILLISECONDS.toHours(screenTimeMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(screenTimeMs) % 60
        binding.screenTimeValue.text = "${hours}h ${minutes}m"
        
        // Calculate comparison with yesterday
        if (yesterdayScreenTimeMs > 0) {
            val difference = screenTimeMs - yesterdayScreenTimeMs
            val percentChange = (difference * 100 / yesterdayScreenTimeMs.toFloat()).toInt()
            
            // Show improvement or increase
            if (difference < 0) {
                // Less screen time than yesterday (improvement)
                binding.trendIcon.setImageResource(R.drawable.ic_check_circle)
                binding.trendIcon.setColorFilter(resources.getColor(R.color.colorSuccess, null))
                binding.comparisonText.text = "${Math.abs(percentChange)}% vs this point yesterday"
                
                val minsDiff = TimeUnit.MILLISECONDS.toMinutes(Math.abs(difference))
                binding.improvementText.text = "${minsDiff}m less - you're doing amazing"
            } else {
                // More screen time than yesterday
                binding.trendIcon.setImageResource(R.drawable.ic_warning)
                binding.trendIcon.setColorFilter(resources.getColor(R.color.colorWarning, null))
                binding.comparisonText.text = "${percentChange}% vs this point yesterday"
                
                val minsDiff = TimeUnit.MILLISECONDS.toMinutes(difference)
                binding.improvementText.text = "${minsDiff}m more - try to reduce usage"
            }
        } else {
            // No comparison data available
            binding.comparisonContainer.visibility = View.GONE
            binding.improvementText.visibility = View.GONE
        }
    }
    
    private fun updateAppUsageUI(appUsageMap: Map<String, AppUsageInfo>) {
        if (appUsageMap.isEmpty()) {
            binding.noDataText.visibility = View.VISIBLE
            binding.appUsageRecyclerView.visibility = View.GONE
        } else {
            binding.noDataText.visibility = View.GONE
            binding.appUsageRecyclerView.visibility = View.VISIBLE
            
            // Convert to list and update adapter
            val appUsageList = appUsageMap.values.toList()
            appUsageAdapter.submitList(appUsageList)
        }
    }
    
    private fun getAppName(packageName: String): String {
        try {
            val packageManager = requireContext().packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            return packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            Log.e("ScreenTimeFragment", "Error getting app name for $packageName", e)
            return packageName.split(".").last()
        }
    }
    
    private fun getAppCategory(packageName: String): String {
        // In a real app, you would categorize apps based on their category in Play Store
        // For simplicity, we'll use some predefined categories
        return when {
            packageName.contains("instagram") || 
            packageName.contains("facebook") || 
            packageName.contains("twitter") || 
            packageName.contains("snapchat") || 
            packageName.contains("discord") -> "Social"
            
            packageName.contains("game") || 
            packageName.contains("play") -> "Games"
            
            packageName.contains("netflix") || 
            packageName.contains("youtube") || 
            packageName.contains("spotify") || 
            packageName.contains("music") -> "Entertainment"
            
            packageName.contains("gmail") || 
            packageName.contains("outlook") || 
            packageName.contains("mail") -> "Productivity"
            
            else -> "Other"
        }
    }
    
    private fun showNoPermissionState() {
        binding.screenTimeValue.text = "--"
        binding.comparisonContainer.visibility = View.GONE
        binding.improvementText.text = "Usage permission required"
        binding.noDataText.visibility = View.VISIBLE
        binding.appUsageRecyclerView.visibility = View.GONE
    }
    
    private fun showErrorState() {
        binding.screenTimeValue.text = "Error"
        binding.comparisonContainer.visibility = View.GONE
        binding.improvementText.text = "Could not load screen time data"
        binding.noDataText.visibility = View.VISIBLE
        binding.appUsageRecyclerView.visibility = View.GONE
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // Data class to hold app usage information
    data class AppUsageInfo(
        val packageName: String,
        val appName: String,
        val category: String,
        var usageTimeMs: Long
    )
}
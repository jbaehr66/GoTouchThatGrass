package com.example.gotouchthatgrass_3.ui.blocked

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.gotouchthatgrass_3.R
import com.example.gotouchthatgrass_3.databinding.FragmentBlockedAppsBinding
import com.example.gotouchthatgrass_3.MainApplication
import com.example.gotouchthatgrass_3.service.AppBlockerService
import com.example.gotouchthatgrass_3.ui.theme.FontCache
import com.example.gotouchthatgrass_3.util.ClickUtils
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.tabs.TabLayoutMediator

class BlockedAppsFragment : Fragment() {

    private var _binding: FragmentBlockedAppsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BlockedAppsViewModel
    private lateinit var appAdapter: AppListAdapter
    private lateinit var blockedAppAdapter: BlockedAppAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(BlockedAppsViewModel::class.java)
        _binding = FragmentBlockedAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModels
        viewModel = ViewModelProvider(this).get(BlockedAppsViewModel::class.java)
        val creditsViewModel = ViewModelProvider(this).get(CreditsViewModel::class.java)

        // Display available credits
        creditsViewModel.availableCreditsText.observe(viewLifecycleOwner) { creditsText ->
            binding.creditsTitle.text = creditsText
        }
        
        // Setup get more credits button
        binding.btnGetMoreCredits.setOnClickListener {
            Toast.makeText(
                context, 
                "Complete grass detection challenges to earn more credits!", 
                Toast.LENGTH_SHORT
            ).show()
        }
        
        try {
            // Apply fonts to the credit display
            BlockedAppsHelper.applyFontToView(requireContext(), binding.creditsTitle, R.font.poppins_bold)
            BlockedAppsHelper.applyFontToView(requireContext(), binding.btnGetMoreCredits, R.font.poppins_medium)
            
            // Use the Application-wide utility to apply fonts to the entire view hierarchy
            // This replaces the XML fontFamily attributes that cause StrictMode violations
            MainApplication.applyFontsSafely(requireContext(), binding.root)
        } catch (e: Exception) {
            Log.e("BlockedAppsFragment", "Error loading fonts", e)
        }
        
        // Set up ViewPager with tabs
        setupViewPager()
        
        // Start app blocker service
        requireActivity().startService(Intent(requireContext(), AppBlockerService::class.java))
        
        // Add a badge to the bottom navigation "Blocked Apps" tab
        setupBottomNavBadge()
    }
    
    private fun setupViewPager() {
        // Set up the adapter for the ViewPager
        val tabAdapter = BlockedAppsTabAdapter(this)
        binding.viewPager.adapter = tabAdapter
        
        // Connect the TabLayout with the ViewPager
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.blocked_apps_tab)
                1 -> getString(R.string.available_apps_tab)
                else -> null
            }
        }.attach()
        
        try {
            // Add a badge to the blocked apps tab
            val blockedAppsBadge = binding.tabLayout.getTabAt(0)?.orCreateBadge!!
            blockedAppsBadge.isVisible = false
            blockedAppsBadge.backgroundColor = resources.getColor(R.color.colorSecondary, null)
            
            // Update the badge count when blocked apps change
            viewModel.blockedApps.observe(viewLifecycleOwner) { blockedApps ->
                val blockedCount = blockedApps.count { it.isCurrentlyBlocked }
                if (blockedCount > 0) {
                    blockedAppsBadge.isVisible = true
                    blockedAppsBadge.number = blockedCount
                } else {
                    blockedAppsBadge.isVisible = false
                }
            }
        } catch (e: Exception) {
            Log.e("BlockedAppsFragment", "Error setting up tab badge", e)
        }
    }
    
    private fun setupBottomNavBadge() {
        // Add a badge to the bottom navigation item
        try {
            val navView = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                R.id.nav_view
            )
            val navBlockedAppsBadge = navView.getOrCreateBadge(R.id.navigation_blocked_apps)
            
            // Update the badge count when blocked apps change
            viewModel.blockedApps.observe(viewLifecycleOwner) { blockedApps ->
                val blockedCount = blockedApps.count { it.isCurrentlyBlocked }
                if (blockedCount > 0) {
                    navBlockedAppsBadge.isVisible = true
                    navBlockedAppsBadge.number = blockedCount
                } else {
                    navBlockedAppsBadge.isVisible = false
                }
            }
        } catch (e: Exception) {
            Log.e("BlockedAppsFragment", "Failed to set up bottom nav badge", e)
        }
    }

    // This method is no longer needed as it's moved to the tab fragments
    private fun setupAppList() {
        // Moved to AvailableAppsTabFragment
        appAdapter = AppListAdapter { app ->
            viewModel.blockApp(app.packageName, app.label.toString())
        }
        
        // Note: This code is commented out because recyclerViewApps is no longer in this layout
        // It has been moved to the individual tab fragments
        /*
        binding.recyclerViewApps.apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = appAdapter
            
            // Ensure recycler view is visible
            visibility = View.VISIBLE
            
            // Add extra space at bottom for better scrolling experience
            clipToPadding = false
            setPadding(paddingLeft, paddingTop, paddingRight, resources.getDimensionPixelSize(R.dimen.recycler_bottom_padding))
        }

        try {
            // First attempt to load real apps
            loadRealApps()
        } catch (e: Exception) {
            Log.e("BlockedAppsFragment", "Error loading real apps, falling back to sample data", e)
            // Fall back to sample data if there's any issue
            loadSampleApps()
        }
        */
    }
    
    /**
     * Loads actual installed apps from the device
     */
    private fun loadRealApps() {
        // Load installed apps with better diagnostic logging
        val packageManager = requireContext().packageManager
        val allApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        Log.d("BlockedAppsFragment", "Found ${allApps.size} total apps")
        
        // First get all non-system apps
        val nonSystemApps = allApps.filter { app ->
            (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        }
        Log.d("BlockedAppsFragment", "Found ${nonSystemApps.size} non-system apps")
        
        // Then filter out our own app and apps without launchers
        val launchableApps = nonSystemApps.filter { app ->
            val isOurApp = app.packageName == requireContext().packageName
            val hasLauncher = packageManager.getLaunchIntentForPackage(app.packageName) != null
            val includeApp = !isOurApp && hasLauncher
            
            // Log some example apps to verify filter logic
            if (nonSystemApps.indexOf(app) < 5) {
                Log.d("BlockedAppsFragment", "App: ${app.packageName}, isOurApp: $isOurApp, hasLauncher: $hasLauncher, includeApp: $includeApp")
            }
            
            includeApp
        }
        Log.d("BlockedAppsFragment", "Found ${launchableApps.size} launchable apps to display")
        
        // Create app items for display
        val installedApps = launchableApps.map { app ->
            try {
                val label = packageManager.getApplicationLabel(app)
                val icon = packageManager.getApplicationIcon(app.packageName)
                AppItem(
                    packageName = app.packageName,
                    label = label,
                    icon = icon
                )
            } catch (e: Exception) {
                Log.e("BlockedAppsFragment", "Error loading app info for ${app.packageName}", e)
                null
            }
        }.filterNotNull()
            .sortedBy { it.label.toString().lowercase() }
        
        Log.d("BlockedAppsFragment", "Final list contains ${installedApps.size} apps")
        
        if (installedApps.isEmpty()) {
            Log.w("BlockedAppsFragment", "No real apps found, falling back to sample data")
            loadSampleApps()
        } else {
            Log.d("BlockedAppsFragment", "Submitting list of ${installedApps.size} real apps to adapter")
            
            // Get some example app names for logging
            val exampleApps = installedApps.take(5).joinToString(", ") { it.label.toString() }
            Log.d("BlockedAppsFragment", "Example apps: $exampleApps")
            
            // Submit the list to the adapter
            appAdapter.submitList(installedApps)
        }
    }
    
    /**
     * Loads sample apps as a fallback when real apps can't be loaded
     */
    private fun loadSampleApps() {
        Log.d("BlockedAppsFragment", "Loading sample apps")
        
        val defaultIcon = resources.getDrawable(R.drawable.ic_notification, null)
        
        // Create a list of sample apps
        val sampleApps = listOf(
            AppItem("com.android.chrome", "Chrome Browser", defaultIcon),
            AppItem("com.facebook.katana", "Facebook", defaultIcon),
            AppItem("com.instagram.android", "Instagram", defaultIcon),
            AppItem("com.twitter.android", "Twitter", defaultIcon),
            AppItem("com.whatsapp", "WhatsApp", defaultIcon),
            AppItem("com.spotify.music", "Spotify", defaultIcon),
            AppItem("com.netflix.mediaclient", "Netflix", defaultIcon),
            AppItem("com.amazon.shopping", "Amazon Shopping", defaultIcon),
            AppItem("com.google.android.youtube", "YouTube", defaultIcon),
            AppItem("com.snapchat.android", "Snapchat", defaultIcon),
            AppItem("com.tiktok.musically", "TikTok", defaultIcon),
            AppItem("com.reddit.frontpage", "Reddit", defaultIcon),
            AppItem("com.pinterest", "Pinterest", defaultIcon),
            AppItem("com.discord", "Discord", defaultIcon),
            AppItem("com.slack", "Slack", defaultIcon)
        )
        
        Log.d("BlockedAppsFragment", "Created ${sampleApps.size} sample apps")
        
        // Submit the list to the adapter
        appAdapter.submitList(sampleApps)
        
        // Show a toast to notify the user
        Toast.makeText(
            requireContext(),
            "Using sample apps list for demonstration",
            Toast.LENGTH_LONG
        ).show()
    }

    // This method is no longer needed as it's moved to the tab fragments
    private fun setupBlockedAppsList() {
        // Moved to BlockedAppsTabFragment
        blockedAppAdapter = BlockedAppAdapter { app ->
            viewModel.unblockApp(app.packageName)
        }

        // Note: This code is commented out because recyclerViewBlockedApps is no longer in this layout
        // It has been moved to the BlockedAppsTabFragment
        /*
        binding.recyclerViewBlockedApps.apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = blockedAppAdapter
        }

        viewModel.blockedApps.observe(viewLifecycleOwner) { blockedApps ->
            val packageManager = requireContext().packageManager
            val blockedAppItems = blockedApps.map { app ->
                try {
                    BlockedAppItem(
                        packageName = app.packageName,
                        appName = app.appName,
                        icon = packageManager.getApplicationIcon(app.packageName)
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    // App was uninstalled but still in blocked list
                    viewModel.unblockApp(app.packageName)
                    null
                }
            }.filterNotNull()

            blockedAppAdapter.submitList(blockedAppItems)

            // Update UI visibility based on blocked apps list
            if (blockedAppItems.isEmpty()) {
                binding.blockedAppsTitle.visibility = View.GONE
                binding.recyclerViewBlockedApps.visibility = View.GONE
                binding.btnUnblockAll.visibility = View.GONE
                binding.noBlockedAppsText.visibility = View.VISIBLE
            } else {
                binding.blockedAppsTitle.visibility = View.VISIBLE
                binding.recyclerViewBlockedApps.visibility = View.VISIBLE
                binding.btnUnblockAll.visibility = View.VISIBLE
                binding.noBlockedAppsText.visibility = View.GONE
            }
        }
        */
    }

    // This method is no longer needed as it's moved to the tab fragments
    private fun setupSearch() {
        // Moved to AvailableAppsTabFragment
        // Note: This code is commented out because searchView is no longer in this layout
        /*
        binding.searchView.apply {
            // Set up search behavior
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    Log.d("BlockedAppsFragment", "Search submitted: ${query ?: "empty"}")
                    appAdapter.filter(query ?: "")
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    Log.d("BlockedAppsFragment", "Search text changed: ${newText ?: "empty"}")
                    val query = newText ?: ""
                    appAdapter.filter(query)
                    
                    // Update the UI to show message when no results
                    // Use a short delay to ensure adapter has updated
                    binding.recyclerViewApps.postDelayed({
                        val itemCount = appAdapter.itemCount
                        Log.d("BlockedAppsFragment", "After filter, item count: $itemCount")
                        updateEmptySearchState(query, itemCount)
                    }, 100) // Short delay to ensure adapter has updated
                    
                    return true
                }
            })
            
            // Improve search discoverability
            queryHint = "Search applications..."
            isIconified = false
            
            // Don't automatically focus search on fragment creation
            clearFocus()
        }
        
        // Log initial state of app list for debugging
        Log.d("BlockedAppsFragment", "Initial app list size: ${appAdapter.itemCount}")
        */
    }
    
    /**
     * Updates UI to show a message when no search results are found
     * This method is no longer needed as it's moved to the tab fragments
     */
    private fun updateEmptySearchState(query: String, resultCount: Int) {
        // Moved to AvailableAppsTabFragment
        // Note: This code is commented out because the views are no longer in this layout
        /*
        if (query.isNotEmpty() && resultCount == 0) {
            // No results for a search query - show the empty message
            binding.noSearchResultsText.visibility = View.VISIBLE
            binding.recyclerViewApps.visibility = View.GONE
            
            // Update text to be more specific
            binding.noSearchResultsText.text = "No apps matching \"$query\" found"
            
            Log.d("BlockedAppsFragment", "No search results for query: $query")
        } else {
            // We have results or no query - show the list
            binding.noSearchResultsText.visibility = View.GONE
            binding.recyclerViewApps.visibility = View.VISIBLE
            
            Log.d("BlockedAppsFragment", "Search results displayed: $resultCount items")
        }
        */
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    /**
     * Adapter for the ViewPager containing the blocked and available apps tabs
     */
    private inner class BlockedAppsTabAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> BlockedAppsTabFragment()
                1 -> AvailableAppsTabFragment()
                else -> throw IllegalArgumentException("Invalid tab position: $position")
            }
        }
    }
}

// Helper classes for the adapters
data class AppItem(
    val packageName: String,
    val label: CharSequence,
    val icon: android.graphics.drawable.Drawable,
    val category: com.example.gotouchthatgrass_3.models.AppCategory = com.example.gotouchthatgrass_3.models.AppCategory.OTHER
) {
    override fun toString(): String {
        return label.toString()
    }
}

data class BlockedAppItem(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable,
    val blockTime: Long = System.currentTimeMillis() // timestamp when the app was blocked
) {
    override fun toString(): String {
        return appName
    }
}

// Using the standalone AppListAdapter class from AppListAdapter.kt

// Using the standalone BlockedAppAdapter class from BlockedAppAdapter.kt
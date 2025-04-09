package com.example.gotouchthatgrass_3.ui.blocked

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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gotouchthatgrass_3.R
import com.example.gotouchthatgrass_3.databinding.FragmentAvailableAppsTabBinding
import com.example.gotouchthatgrass_3.models.AppCategory
import com.google.android.material.chip.Chip

/**
 * Fragment for the "Available Apps" tab that shows apps that can be blocked.
 */
class AvailableAppsTabFragment : Fragment() {

    private var _binding: FragmentAvailableAppsTabBinding? = null
    private var _bindingExt: FragmentAvailableAppsTabBindingExt? = null
    
    // Use the binding extension which has the chip properties
    private val binding get() = _bindingExt!!

    private lateinit var viewModel: BlockedAppsViewModel
    private lateinit var appAdapter: AppListAdapter
    
    private var currentCategory: AppCategory? = null
    private var currentSearchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAvailableAppsTabBinding.inflate(inflater, container, false)
        _bindingExt = FragmentAvailableAppsTabBindingExt(_binding!!)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get ViewModels from parent fragment
        viewModel = ViewModelProvider(requireParentFragment()).get(BlockedAppsViewModel::class.java)
        
        // Set up the app list
        setupAppList()
        
        // Set up search functionality
        setupSearch()
        
        // Set up category filtering
        setupCategoryFiltering()
        
        // Apply fonts
        try {
            BlockedAppsHelper.applyFontToView(requireContext(), binding.availableAppsTitle, R.font.poppins_bold)
            BlockedAppsHelper.applyFontToView(requireContext(), binding.noSearchResultsText, R.font.poppins_regular)
        } catch (e: Exception) {
            Log.e("AvailableAppsTabFragment", "Error applying fonts", e)
        }
    }
    
    private fun setupAppList() {
        appAdapter = AppListAdapter { app ->
            // Block the selected app
            viewModel.blockApp(app.packageName, app.label.toString())
            
            Toast.makeText(
                requireContext(),
                "Blocked ${app.label}",
                Toast.LENGTH_SHORT
            ).show()
        }
        
        binding.recyclerViewApps.apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = appAdapter
            
            // Add extra space at bottom for better scrolling experience
            clipToPadding = false
            setPadding(paddingLeft, paddingTop, paddingRight, resources.getDimensionPixelSize(R.dimen.recycler_bottom_padding))
        }
        
        try {
            // Load installed apps
            loadInstalledApps()
        } catch (e: Exception) {
            Log.e("AvailableAppsTabFragment", "Error loading apps, falling back to sample data", e)
            loadSampleApps()
        }
    }
    
    private fun setupSearch() {
        binding.searchView.apply {
            // Set up search behavior
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    Log.d("AvailableAppsTabFragment", "Search submitted: ${query ?: "empty"}")
                    applyFilters(query ?: "")
                    return true
                }
                
                override fun onQueryTextChange(newText: String?): Boolean {
                    Log.d("AvailableAppsTabFragment", "Search text changed: ${newText ?: "empty"}")
                    applyFilters(newText ?: "")
                    return true
                }
            })
            
            // Improve search appearance
            queryHint = getString(R.string.search_apps)
            isIconified = false
            
            // Don't automatically focus search on fragment creation
            clearFocus()
        }
    }
    
    private fun setupCategoryFiltering() {
        // Set up filter button to show/hide category chips
        binding.filterButton.setOnClickListener {
            toggleCategoryVisibility()
        }
        
        // Create category chips dynamically
        createCategoryChips()
        
        // Set up chip group listener
        binding.categoryChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chipId = checkedIds.first()
                
                currentCategory = when (chipId) {
                    binding.chipAllCategories.id -> null
                    binding.chipSocial.id -> AppCategory.SOCIAL
                    binding.chipGames.id -> AppCategory.GAMES
                    binding.chipProductivity.id -> AppCategory.PRODUCTIVITY
                    binding.chipEntertainment.id -> AppCategory.ENTERTAINMENT
                    binding.chipCommunication.id -> AppCategory.COMMUNICATION
                    binding.chipUtilities.id -> AppCategory.UTILITIES
                    binding.chipOther.id -> AppCategory.OTHER
                    else -> null
                }
                
                // Apply both category and search filters
                applyFilters(currentSearchQuery)
            }
        }
    }
    
    private fun toggleCategoryVisibility() {
        val currentVisibility = binding.categoryChipsScroll.visibility
        binding.categoryChipsScroll.visibility = if (currentVisibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }
    
    private fun createCategoryChips() {
        // The "All Categories" chip is already in the layout
        BlockedAppsHelper.applyFontToView(requireContext(), binding.chipAllCategories, R.font.poppins_regular)
        
        // Add a chip for each category
        AppCategory.values().forEach { category ->
            if (category != AppCategory.OTHER) { // OTHER will be the last one
                addCategoryChip(category)
            }
        }
        
        // Add OTHER as the last category
        addCategoryChip(AppCategory.OTHER)
    }
    
    private fun addCategoryChip(category: AppCategory) {
        val chip = layoutInflater.inflate(
            R.layout.chip_category, 
            binding.categoryChipGroup,
            false
        ) as Chip
        
        chip.text = category.displayName
        chip.id = View.generateViewId()
        
        // Apply font to the chip
        BlockedAppsHelper.applyFontToView(requireContext(), chip, R.font.poppins_regular)
        
        // Store the chip in our binding extension
        when (category) {
            AppCategory.SOCIAL -> binding.chipSocial = chip
            AppCategory.GAMES -> binding.chipGames = chip
            AppCategory.PRODUCTIVITY -> binding.chipProductivity = chip
            AppCategory.ENTERTAINMENT -> binding.chipEntertainment = chip
            AppCategory.COMMUNICATION -> binding.chipCommunication = chip
            AppCategory.UTILITIES -> binding.chipUtilities = chip
            AppCategory.OTHER -> binding.chipOther = chip
        }
        
        binding.categoryChipGroup.addView(chip)
    }
    
    private fun applyFilters(searchQuery: String) {
        currentSearchQuery = searchQuery
        
        // First filter by search query
        appAdapter.filter(searchQuery)
        
        // Then apply category filter if needed
        if (currentCategory != null) {
            val filteredList = appAdapter.currentList.filter { app ->
                app.category == currentCategory
            }
            
            // Apply the category filter
            appAdapter.submitList(filteredList)
        }
        
        // Update UI based on results
        binding.recyclerViewApps.postDelayed({
            updateEmptySearchState()
        }, 100)
    }
    
    private fun updateEmptySearchState() {
        val hasResults = appAdapter.itemCount > 0
        val hasQuery = currentSearchQuery.isNotEmpty() || currentCategory != null
        
        if (!hasResults && hasQuery) {
            // No results for a search or category filter
            binding.noSearchResultsText.visibility = View.VISIBLE
            binding.recyclerViewApps.visibility = View.GONE
            
            // Update message based on filter type
            binding.noSearchResultsText.text = when {
                currentSearchQuery.isNotEmpty() && currentCategory != null ->
                    "No ${currentCategory!!.displayName.lowercase()} apps matching \"$currentSearchQuery\" found"
                currentSearchQuery.isNotEmpty() ->
                    "No apps matching \"$currentSearchQuery\" found"
                currentCategory != null ->
                    "No ${currentCategory!!.displayName.lowercase()} apps found"
                else ->
                    "No apps found"
            }
        } else {
            // We have results or no filter
            binding.noSearchResultsText.visibility = View.GONE
            binding.recyclerViewApps.visibility = View.VISIBLE
        }
    }
    
    /**
     * Loads actual installed apps from the device
     */
    private fun loadInstalledApps() {
        val packageManager = requireContext().packageManager
        val allApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        Log.d("AvailableAppsTabFragment", "Found ${allApps.size} total apps")
        
        // Get non-system apps with launchers
        val nonSystemApps = allApps.filter { app ->
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isOurApp = app.packageName == requireContext().packageName
            val hasLauncher = packageManager.getLaunchIntentForPackage(app.packageName) != null
            
            !isSystem && !isOurApp && hasLauncher
        }
        
        // Create app items with categories
        val installedApps = nonSystemApps.map { app ->
            try {
                val label = packageManager.getApplicationLabel(app)
                val icon = packageManager.getApplicationIcon(app.packageName)
                
                // Determine app category
                val category = AppCategory.categorizeApp(app.packageName, label.toString())
                
                AppItem(
                    packageName = app.packageName,
                    label = label,
                    icon = icon,
                    category = category
                )
            } catch (e: Exception) {
                Log.e("AvailableAppsTabFragment", "Error loading app info for ${app.packageName}", e)
                null
            }
        }.filterNotNull()
            .sortedBy { it.label.toString().lowercase() }
        
        Log.d("AvailableAppsTabFragment", "Final list contains ${installedApps.size} apps")
        
        if (installedApps.isEmpty()) {
            Log.w("AvailableAppsTabFragment", "No real apps found, falling back to sample data")
            loadSampleApps()
        } else {
            appAdapter.submitList(installedApps)
        }
    }
    
    /**
     * Loads sample apps as a fallback
     */
    private fun loadSampleApps() {
        Log.d("AvailableAppsTabFragment", "Loading sample apps")
        
        val defaultIcon = resources.getDrawable(R.drawable.ic_notification, null)
        
        // Create a list of sample apps with categories
        val sampleApps = listOf(
            AppItem("com.android.chrome", "Chrome Browser", defaultIcon, AppCategory.UTILITIES),
            AppItem("com.facebook.katana", "Facebook", defaultIcon, AppCategory.SOCIAL),
            AppItem("com.instagram.android", "Instagram", defaultIcon, AppCategory.SOCIAL),
            AppItem("com.twitter.android", "Twitter", defaultIcon, AppCategory.SOCIAL),
            AppItem("com.whatsapp", "WhatsApp", defaultIcon, AppCategory.COMMUNICATION),
            AppItem("com.spotify.music", "Spotify", defaultIcon, AppCategory.ENTERTAINMENT),
            AppItem("com.netflix.mediaclient", "Netflix", defaultIcon, AppCategory.ENTERTAINMENT),
            AppItem("com.amazon.shopping", "Amazon Shopping", defaultIcon, AppCategory.UTILITIES),
            AppItem("com.google.android.youtube", "YouTube", defaultIcon, AppCategory.ENTERTAINMENT),
            AppItem("com.snapchat.android", "Snapchat", defaultIcon, AppCategory.SOCIAL),
            AppItem("com.tiktok.musically", "TikTok", defaultIcon, AppCategory.SOCIAL),
            AppItem("com.reddit.frontpage", "Reddit", defaultIcon, AppCategory.SOCIAL),
            AppItem("com.pinterest", "Pinterest", defaultIcon, AppCategory.SOCIAL),
            AppItem("com.discord", "Discord", defaultIcon, AppCategory.COMMUNICATION),
            AppItem("com.slack", "Slack", defaultIcon, AppCategory.PRODUCTIVITY)
        )
        
        appAdapter.submitList(sampleApps)
        
        Toast.makeText(
            requireContext(),
            "Using sample apps list for demonstration",
            Toast.LENGTH_LONG
        ).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _bindingExt = null
    }
}
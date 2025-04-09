package com.example.gotouchthatgrass_3.ui.blocked

import com.example.gotouchthatgrass_3.databinding.FragmentAvailableAppsTabBinding
import com.google.android.material.chip.Chip

/**
 * Extension properties for FragmentAvailableAppsTabBinding to make it easier to
 * work with dynamically created category chips.
 */
class FragmentAvailableAppsTabBindingExt(private val binding: FragmentAvailableAppsTabBinding) {
    // Category chips
    var chipSocial: Chip = Chip(binding.root.context)
    var chipGames: Chip = Chip(binding.root.context)
    var chipProductivity: Chip = Chip(binding.root.context)
    var chipEntertainment: Chip = Chip(binding.root.context)
    var chipCommunication: Chip = Chip(binding.root.context)
    var chipUtilities: Chip = Chip(binding.root.context)
    var chipOther: Chip = Chip(binding.root.context)
    
    // Delegate all other properties to the original binding
    val root get() = binding.root
    val availableAppsTitle get() = binding.availableAppsTitle
    val filterButton get() = binding.filterButton
    val searchView get() = binding.searchView
    val categoryChipsScroll get() = binding.categoryChipsScroll
    val categoryChipGroup get() = binding.categoryChipGroup
    val recyclerViewApps get() = binding.recyclerViewApps
    val noSearchResultsText get() = binding.noSearchResultsText
    
    // The "All Categories" chip is already in the layout
    val chipAllCategories get() = binding.chipAllCategories
}
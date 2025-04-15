package com.example.gotouchthatgrass_3.ui.blocked

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gotouchthatgrass_3.R
import com.example.gotouchthatgrass_3.databinding.FragmentBlockedAppsTabBinding
import com.example.gotouchthatgrass_3.models.BlockedAppItem
import com.example.gotouchthatgrass_3.ui.theme.FontCache
import com.example.gotouchthatgrass_3.util.ClickUtils
import com.example.gotouchthatgrass_3.util.TimeUtils

/**
 * Fragment for the "Blocked Apps" tab that shows currently blocked apps.
 */
class BlockedAppsTabFragment : Fragment() {

    private var _binding: FragmentBlockedAppsTabBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BlockedAppsViewModel
    private lateinit var creditsViewModel: CreditsViewModel
    private lateinit var blockedAppAdapter: BlockedAppAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBlockedAppsTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get ViewModels from parent fragment
        viewModel = ViewModelProvider(requireParentFragment()).get(BlockedAppsViewModel::class.java)
        creditsViewModel = ViewModelProvider(requireParentFragment()).get(CreditsViewModel::class.java)

        // Set up the "Unblock All" button
        setupUnblockAllButton()

        // Set up the blocked apps RecyclerView
        setupBlockedAppsList()

        // Apply fonts
        try {
            BlockedAppsHelper.applyFontToView(requireContext(), binding.blockedAppsTitle, R.font.poppins_bold)
            BlockedAppsHelper.applyFontToView(requireContext(), binding.noBlockedAppsText, R.font.poppins_regular)
            BlockedAppsHelper.applyFontToView(requireContext(), binding.btnUnblockAll, R.font.poppins_medium)
        } catch (e: Exception) {
            Log.e("BlockedAppsTabFragment", "Error applying fonts", e)
        }
    }

    private fun setupUnblockAllButton() {
        // Check if user can afford to unblock all apps
        creditsViewModel.canAffordUnblockAll().observe(viewLifecycleOwner) { canAfford ->
            binding.btnUnblockAll.isEnabled = canAfford
            binding.btnUnblockAll.alpha = if (canAfford) 1.0f else 0.5f
        }

        // Set up click listener
        ClickUtils.setDebouncedClickListener(binding.btnUnblockAll, 1000) {
            // Check if we can afford it again (for safety)
            if (creditsViewModel.canAffordUnblockAll().value == true) {
                // Spend credits first
                creditsViewModel.spendCreditsForUnblockAll()
                
                // Then unblock all apps
                viewModel.unblockAllApps()
                
                Toast.makeText(
                    requireContext(),
                    R.string.unblock_all,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.insufficient_credits,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupBlockedAppsList() {
        // Initialize the adapter with unblock click handler
        blockedAppAdapter = BlockedAppAdapter { blockedApp ->
            try {
                // Check if we have enough credits to unblock
                creditsViewModel.getUnblockCost(blockedApp.packageName).observe(viewLifecycleOwner) { cost ->
                    val availableCredits = creditsViewModel.userCredits.value?.availableCredits ?: 0
                    val canAfford = availableCredits >= cost
                    
                    Log.d("BlockedAppsTabFragment", "Unblock attempt for ${blockedApp.appName}: Cost=$cost, Available=$availableCredits, CanAfford=$canAfford")
                    
                    if (canAfford) {
                        try {
                            // Spend credits first
                            creditsViewModel.spendCreditsForUnblock(blockedApp.packageName)
                            
                            // Then unblock the app
                            viewModel.unblockApp(blockedApp.packageName)
                            
                            Toast.makeText(
                                requireContext(),
                                "Unblocked ${blockedApp.appName}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            Log.e("BlockedAppsTabFragment", "Error unblocking app: ${blockedApp.packageName}", e)
                            Toast.makeText(
                                requireContext(),
                                "Error unblocking app",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        // Show insufficient credits message
                        Toast.makeText(
                            requireContext(),
                            R.string.insufficient_credits,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("BlockedAppsTabFragment", "Error in unblock click handler", e)
                Toast.makeText(requireContext(), "Error processing request", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up the RecyclerView
        binding.recyclerViewBlockedApps.apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = blockedAppAdapter
        }

        // Observe blocked apps from the ViewModel
        viewModel.blockedApps.observe(viewLifecycleOwner) { blockedApps ->
            try {
                Log.d("BlockedAppsTabFragment", "Received ${blockedApps.size} apps, filtering currently blocked ones")
                
                // Filter to only show currently blocked apps
                val currentlyBlockedApps = blockedApps.filter { it.isCurrentlyBlocked }
                Log.d("BlockedAppsTabFragment", "Found ${currentlyBlockedApps.size} currently blocked apps")
                
                // Create blocked app items with credit cost and block time
                val packageManager = requireContext().packageManager
                val blockedAppItems = currentlyBlockedApps.mapNotNull { app ->
                    try {
                        // Try to get the app icon, use a default if not found
                        val icon = try {
                            packageManager.getApplicationIcon(app.packageName)
                        } catch (e: PackageManager.NameNotFoundException) {
                            Log.w("BlockedAppsTabFragment", "App icon not found for ${app.packageName}, using default")
                            resources.getDrawable(R.drawable.ic_notification, null)
                        }
                        
                        val blockTime = app.blockStartTime
                        
                        BlockedAppItem(
                            packageName = app.packageName,
                            appName = app.appName,
                            icon = icon,
                            blockTime = blockTime
                        )
                    } catch (e: Exception) {
                        Log.e("BlockedAppsTabFragment", "Error creating BlockedAppItem for ${app.packageName}", e)
                        // App was uninstalled or has issues, remove from blocked list
                        viewModel.unblockApp(app.packageName)
                        null
                    }
                }

                // Update the adapter with the new list
                blockedAppAdapter.submitList(blockedAppItems)
                Log.d("BlockedAppsTabFragment", "Submitted ${blockedAppItems.size} items to adapter")

                // Show/hide UI elements based on whether there are blocked apps
                if (blockedAppItems.isEmpty()) {
                    binding.recyclerViewBlockedApps.visibility = View.GONE
                    binding.btnUnblockAll.visibility = View.GONE
                    binding.emptyStateContainer.visibility = View.VISIBLE
                    Log.d("BlockedAppsTabFragment", "No blocked apps, showing empty state")
                } else {
                    binding.recyclerViewBlockedApps.visibility = View.VISIBLE
                    binding.btnUnblockAll.visibility = View.VISIBLE
                    binding.emptyStateContainer.visibility = View.GONE
                    Log.d("BlockedAppsTabFragment", "Showing ${blockedAppItems.size} blocked apps")
                }
            } catch (e: Exception) {
                Log.e("BlockedAppsTabFragment", "Error processing blocked apps", e)
                Toast.makeText(requireContext(), "Error loading blocked apps", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
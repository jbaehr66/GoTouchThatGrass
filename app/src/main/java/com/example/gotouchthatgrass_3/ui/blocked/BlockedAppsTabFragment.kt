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
        blockedAppAdapter = BlockedAppAdapter { blockedApp ->
            // Check if we have enough credits to unblock
            creditsViewModel.getUnblockCost(blockedApp.packageName).observe(viewLifecycleOwner) { cost ->
                val canAfford = creditsViewModel.userCredits.value?.availableCredits ?: 0 >= cost
                
                if (canAfford) {
                    // Spend credits first
                    creditsViewModel.spendCreditsForUnblock(blockedApp.packageName)
                    
                    // Then unblock the app
                    viewModel.unblockApp(blockedApp.packageName)
                    
                    Toast.makeText(
                        requireContext(),
                        "Unblocked ${blockedApp.appName}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Show insufficient credits message
                    Toast.makeText(
                        requireContext(),
                        R.string.insufficient_credits,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.recyclerViewBlockedApps.apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = blockedAppAdapter
        }

        viewModel.blockedApps.observe(viewLifecycleOwner) { blockedApps ->
            val currentlyBlockedApps = blockedApps.filter { it.isCurrentlyBlocked }
            
            // Create blocked app items with credit cost and block time
            val packageManager = requireContext().packageManager
            val blockedAppItems = currentlyBlockedApps.map { app ->
                try {
                    val icon = packageManager.getApplicationIcon(app.packageName)
                    val blockTime = app.blockStartTime
                    
                    BlockedAppItem(
                        packageName = app.packageName,
                        appName = app.appName,
                        icon = icon,
                        blockTime = blockTime
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    // App was uninstalled but still in blocked list
                    viewModel.unblockApp(app.packageName)
                    null
                }
            }.filterNotNull()

            // Update the adapter with the new list
            blockedAppAdapter.submitList(blockedAppItems)

            // Show/hide UI elements based on whether there are blocked apps
            if (blockedAppItems.isEmpty()) {
                binding.recyclerViewBlockedApps.visibility = View.GONE
                binding.btnUnblockAll.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.VISIBLE
            } else {
                binding.recyclerViewBlockedApps.visibility = View.VISIBLE
                binding.btnUnblockAll.visibility = View.VISIBLE
                binding.emptyStateContainer.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
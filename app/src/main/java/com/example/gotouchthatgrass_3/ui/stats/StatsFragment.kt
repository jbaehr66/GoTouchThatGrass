package com.example.gotouchthatgrass_3.ui.stats

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gotouchthatgrass_3.R
import com.example.gotouchthatgrass_3.databinding.FragmentStatsBinding
import com.example.gotouchthatgrass_3.models.Challenge
import com.example.gotouchthatgrass_3.ui.theme.FontHelper
import com.example.gotouchthatgrass_3.ui.theme.FontLoader
import com.example.gotouchthatgrass_3.util.PreferenceManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Extension property to check if a view is attached to window
 * Used to prevent updating recycled views
 */
val View.isAttachedToWindow: Boolean
    get() = windowToken != null

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private lateinit var statsViewModel: StatsViewModel
    private lateinit var adapter: ChallengeHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        statsViewModel = ViewModelProvider(this).get(StatsViewModel::class.java)

        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            // Load fonts asynchronously using FontHelper
            FontHelper.applyFontsToStatsFragment(this, binding)
        } catch (e: Exception) {
            Log.e("StatsFragment", "Error loading fonts", e)
        }
        
        setupRecyclerView()
        displayCurrentStreak()
        observeChallenges()
    }

    private fun setupRecyclerView() {
        adapter = ChallengeHistoryAdapter()
        binding.challengeHistoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.challengeHistoryRecyclerView.adapter = adapter
    }

    private fun displayCurrentStreak() {
        val preferenceManager = PreferenceManager(requireContext())
        binding.streakCountText.text = preferenceManager.streak.toString()

        // Get the last challenge date
        val timestamp = preferenceManager.lastChallengeTimestamp
        if (timestamp > 0) {
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val dateString = dateFormat.format(Date(timestamp))
            binding.lastChallengeText.text = getString(R.string.last_challenge_date, dateString)
        } else {
            binding.lastChallengeText.text = getString(R.string.no_challenges_yet)
        }
    }

    private fun observeChallenges() {
        statsViewModel.getAllChallenges().observe(viewLifecycleOwner) { challenges ->
            if (challenges.isEmpty()) {
                binding.noDataText.visibility = View.VISIBLE
                binding.challengeHistoryRecyclerView.visibility = View.GONE
            } else {
                binding.noDataText.visibility = View.GONE
                binding.challengeHistoryRecyclerView.visibility = View.VISIBLE
                adapter.submitList(challenges)

                // Update total challenges count
                binding.totalChallengesText.text = challenges.size.toString()

                // Calculate success rate
                val successfulChallenges = challenges.count { it.isSuccessful }
                val successRate = if (challenges.isNotEmpty()) {
                    (successfulChallenges * 100 / challenges.size)
                } else {
                    0
                }
                binding.successRateText.text = "$successRate%"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ChallengeHistoryAdapter : androidx.recyclerview.widget.ListAdapter<Challenge, ChallengeHistoryAdapter.ChallengeViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Challenge>() {
        override fun areItemsTheSame(oldItem: Challenge, newItem: Challenge): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Challenge, newItem: Challenge): Boolean {
            return oldItem == newItem
        }
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = com.example.gotouchthatgrass_3.databinding.ItemChallengeHistoryBinding.inflate(inflater, parent, false)
        return ChallengeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChallengeViewHolder(
        private val binding: com.example.gotouchthatgrass_3.databinding.ItemChallengeHistoryBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(challenge: Challenge) {
            // Apply fonts asynchronously with proper caching
            val context = binding.root.context
            
            try {
                // Convert context to LifecycleOwner if possible
                if (context is androidx.lifecycle.LifecycleOwner) {
                    val lifecycleScope = context.lifecycleScope
                    
                    // Get direct references to views to avoid binding property errors
                    val dateTextView = itemView.findViewById<android.widget.TextView>(R.id.challengeDateText)
                    val statusTextView = itemView.findViewById<android.widget.TextView>(R.id.statusText)
                    val notesTextView = itemView.findViewById<android.widget.TextView>(R.id.notesText)
                    
                    // Apply fonts to TextViews
                    if (dateTextView != null) {
                        FontLoader.loadFontAsync(lifecycleScope, context, dateTextView, R.font.poppins_medium)
                    }
                    if (statusTextView != null) {
                        FontLoader.loadFontAsync(lifecycleScope, context, statusTextView, R.font.poppins_regular)
                    }
                    if (notesTextView != null) {
                        FontLoader.loadFontAsync(lifecycleScope, context, notesTextView, R.font.poppins_light)
                    }
                }
            } catch (e: Exception) {
                Log.e("ChallengeViewHolder", "Error loading fonts", e)
            }
            
            // Format date
            val dateFormat = SimpleDateFormat("MMM d, yyyy - h:mm a", Locale.getDefault())
            val dateTextView = itemView.findViewById<android.widget.TextView>(R.id.challengeDateText)
            dateTextView?.text = dateFormat.format(Date(challenge.timestamp))

            // Set status
            val statusTextView = itemView.findViewById<android.widget.TextView>(R.id.statusText)
            if (statusTextView != null) {
                if (challenge.isSuccessful) {
                    statusTextView.text = binding.root.context.getString(R.string.success)
                    statusTextView.setTextColor(binding.root.context.getColor(R.color.success_green))
                } else {
                    statusTextView.text = binding.root.context.getString(R.string.failure)
                    statusTextView.setTextColor(binding.root.context.getColor(R.color.failure_red))
                }
            }

            // Set notes if any
            val notesTextView = itemView.findViewById<android.widget.TextView>(R.id.notesText)
            if (notesTextView != null) {
                if (challenge.notes.isNotEmpty()) {
                    notesTextView.visibility = View.VISIBLE
                    notesTextView.text = challenge.notes
                } else {
                    notesTextView.visibility = View.GONE
                }
            }
            
            // Handle the challenge image safely
            val challengeImageView = itemView.findViewById<android.widget.ImageView>(R.id.challengeImage)
            if (challengeImageView != null) {
                // Set placeholder initially
                challengeImageView.setImageResource(R.drawable.ic_image)
                
                // Don't attempt to load if path is empty
                if (challenge.photoPath.isNotEmpty()) {
                    // Get context and check if it's a LifecycleOwner
                    val context = binding.root.context
                    if (context is androidx.lifecycle.LifecycleOwner) {
                        // Load image on background thread to avoid StrictMode violations
                        context.lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val imgFile = File(challenge.photoPath)
                                val fileExists = if (imgFile != null) {
                                    withContext(Dispatchers.IO) {
                                        imgFile.exists()
                                    }
                                } else {
                                    false
                                }
                                
                                if (fileExists) {
                                    val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                                    withContext(Dispatchers.Main) {
                                        // Check if view is still attached before setting bitmap
                                        if (itemView.isAttachedToWindow) {
                                            challengeImageView.setImageBitmap(bitmap)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("ChallengeViewHolder", "Error loading image", e)
                            }
                        }
                    }
                }
            }
        }
    }
}
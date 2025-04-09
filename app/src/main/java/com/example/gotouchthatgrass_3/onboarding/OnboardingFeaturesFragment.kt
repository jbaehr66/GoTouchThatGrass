package com.example.gotouchthatgrass_3.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.gotouchthatgrass_3.databinding.FragmentOnboardingFeaturesBinding

class OnboardingFeaturesFragment : Fragment() {

    private var _binding: FragmentOnboardingFeaturesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingFeaturesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set up the feature image
        // This would typically load an image resource showing app features
        // binding.ivFeatureImage.setImageResource(R.drawable.feature_image)
        
        // Set up the dots indicator
        setupDotsIndicator()
        
        binding.btnContinue.setOnClickListener {
            // Move to the next page when the user taps the continue button
            val viewPager = activity?.findViewById<androidx.viewpager2.widget.ViewPager2>(com.example.gotouchthatgrass_3.R.id.viewPager)
            viewPager?.currentItem = 3
        }
    }
    
    private fun setupDotsIndicator() {
        // This would typically set up dots to indicate which page the user is on
        // For simplicity, we're not implementing the full dots indicator here
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
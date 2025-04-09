package com.example.gotouchthatgrass_3.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.gotouchthatgrass_3.databinding.FragmentOnboardingMotivationBinding

class OnboardingMotivationFragment : Fragment() {

    private var _binding: FragmentOnboardingMotivationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingMotivationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.root.setOnClickListener {
            // Move to the next page when the user taps anywhere on the screen
            val viewPager = activity?.findViewById<androidx.viewpager2.widget.ViewPager2>(com.example.gotouchthatgrass_3.R.id.viewPager)
            viewPager?.currentItem = 2
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
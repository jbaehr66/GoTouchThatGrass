package com.example.gotouchthatgrass_3.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.gotouchthatgrass_3.OnboardingActivity
import com.example.gotouchthatgrass_3.databinding.FragmentOnboardingGetStartedBinding

class OnboardingGetStartedFragment : Fragment() {

    private var _binding: FragmentOnboardingGetStartedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingGetStartedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set up the app preview image
        // This would typically load an image resource showing the app's main screen
        // binding.ivAppPreview.setImageResource(R.drawable.app_preview)
        
        binding.btnGetStarted.setOnClickListener {
            // Complete onboarding and move to the main activity
            (activity as? OnboardingActivity)?.completeOnboarding()
        }
        
        binding.tvSignIn.setOnClickListener {
            // For now, just complete onboarding as if the user is signing in
            // In a real app, this would navigate to a sign-in screen
            (activity as? OnboardingActivity)?.completeOnboarding()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
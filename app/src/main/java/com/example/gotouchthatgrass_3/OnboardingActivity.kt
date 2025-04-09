package com.example.gotouchthatgrass_3

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.gotouchthatgrass_3.databinding.ActivityOnboardingBinding
import com.example.gotouchthatgrass_3.onboarding.OnboardingFeaturesFragment
import com.example.gotouchthatgrass_3.onboarding.OnboardingGetStartedFragment
import com.example.gotouchthatgrass_3.onboarding.OnboardingMotivationFragment
import com.example.gotouchthatgrass_3.onboarding.OnboardingWelcomeFragment
import com.example.gotouchthatgrass_3.util.PreferenceManager

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var onboardingAdapter: OnboardingAdapter
    private val fragments = listOf(
        OnboardingWelcomeFragment(),
        OnboardingMotivationFragment(),
        OnboardingFeaturesFragment(),
        OnboardingGetStartedFragment()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)

        // Check if this is the first launch
        if (!isFirstLaunch()) {
            navigateToMainActivity()
            return
        }

        setupViewPager()
        setupPageIndicators()
    }

    private fun setupViewPager() {
        onboardingAdapter = OnboardingAdapter(this, fragments)
        binding.viewPager.adapter = onboardingAdapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePageIndicators(position)
            }
        })
    }

    private fun setupPageIndicators() {
        val indicators = Array(fragments.size) { ImageView(this) }

        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(8, 0, 8, 0)

        for (i in indicators.indices) {
            indicators[i] = ImageView(this)
            indicators[i].setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    if (i == 0) R.drawable.indicator_active else R.drawable.indicator_inactive
                )
            )
            indicators[i].layoutParams = layoutParams
            binding.indicatorsContainer.addView(indicators[i])
        }
    }

    private fun updatePageIndicators(position: Int) {
        val childCount = binding.indicatorsContainer.childCount
        for (i in 0 until childCount) {
            val imageView = binding.indicatorsContainer.getChildAt(i) as ImageView
            imageView.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    if (i == position) R.drawable.indicator_active else R.drawable.indicator_inactive
                )
            )
        }
    }

    private fun isFirstLaunch(): Boolean {
        return preferenceManager.isFirstLaunch
    }

    fun completeOnboarding() {
        preferenceManager.isFirstLaunch = false
        navigateToMainActivity()
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    inner class OnboardingAdapter(fa: FragmentActivity, private val fragments: List<Fragment>) :
        FragmentStateAdapter(fa) {

        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment = fragments[position]
    }
}
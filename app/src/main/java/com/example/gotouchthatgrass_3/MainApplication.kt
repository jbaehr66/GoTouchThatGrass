package com.example.gotouchthatgrass_3

import android.app.Application
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.fragment.app.FragmentActivity
import com.example.gotouchthatgrass_3.ui.theme.FontCache
import com.example.gotouchthatgrass_3.ui.theme.FontLayoutInflaterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    override fun onCreate() {
        super.onCreate()
        
        // Set up global error handler
        setupExceptionHandler()
        
        // Enable StrictMode in debug builds
        if (BuildConfig.DEBUG) {
            setupStrictMode()
        }
        
        // Initialize EmojiCompat
        initializeEmojiCompat()
        
        // Preload fonts asynchronously using the new FontCache
        FontCache.preload(this, listOf(
            R.font.poppins_medium,
            R.font.poppins_light,
            R.font.poppins_bold,
            R.font.poppins_regular
        ))
        
        // Set up the activity lifecycle callbacks to override fonts
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {
                Log.d("MainApplication", "Activity created: ${activity.javaClass.simpleName}")
                // Nothing needed here as we override fonts directly in fragments
            }
            
            override fun onActivityStarted(activity: android.app.Activity) {}
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivityStopped(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
    }
    
    /**
     * Utility function to safely apply fonts to views created from XML
     * without causing StrictMode violations
     */
    companion object {
        fun applyFontsSafely(context: Context, rootView: View) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Apply fonts on a background thread to avoid StrictMode violations
                    val mediumFont = FontCache.getFont(context, R.font.poppins_medium)
                    val regularFont = FontCache.getFont(context, R.font.poppins_regular)
                    val lightFont = FontCache.getFont(context, R.font.poppins_light)
                    val boldFont = FontCache.getFont(context, R.font.poppins_bold)
                    
                    rootView.post {
                        try {
                            // Apply fonts on the main thread after they've been loaded
                            if (rootView is ViewGroup) {
                                applyFontsToViewGroup(
                                    rootView,
                                    mediumFont,
                                    regularFont,
                                    lightFont,
                                    boldFont
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("MainApplication", "Error applying fonts to view hierarchy", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainApplication", "Error loading fonts", e)
                }
            }
        }
        
        private fun applyFontsToViewGroup(
            viewGroup: ViewGroup,
            mediumFont: Typeface?,
            regularFont: Typeface?,
            lightFont: Typeface?,
            boldFont: Typeface?
        ) {
            val childCount = viewGroup.childCount
            for (i in 0 until childCount) {
                val child = viewGroup.getChildAt(i)
                
                when (child) {
                    is TextView -> {
                        when {
                            child is Button -> mediumFont?.let { child.typeface = it }
                            child.textSize >= 20f -> boldFont?.let { child.typeface = it }
                            child.textSize >= 18f -> mediumFont?.let { child.typeface = it }
                            child.textSize >= 16f -> regularFont?.let { child.typeface = it }
                            else -> lightFont?.let { child.typeface = it }
                        }
                    }
                    is ViewGroup -> {
                        applyFontsToViewGroup(
                            child,
                            mediumFont,
                            regularFont,
                            lightFont,
                            boldFont
                        )
                    }
                }
            }
        }
    }
    
    private fun initializeEmojiCompat() {
        // Initialize emoji on a background thread to avoid StrictMode violations
        applicationScope.launch(Dispatchers.IO) {
            try {
                val config = BundledEmojiCompatConfig(this@MainApplication)
                config.setReplaceAll(true)
                EmojiCompat.init(config)
                Log.d("MainApplication", "EmojiCompat initialized successfully")
            } catch (e: Exception) {
                Log.e("MainApplication", "Failed to initialize EmojiCompat", e)
            }
        }
    }
    
    private fun setupExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // Log crash details to a file
                logCrashToFile(throwable)
                
                // Show a friendly crash notification next time app opens
                val preferences = getSharedPreferences("crash_prefs", MODE_PRIVATE)
                preferences.edit().putBoolean("has_crashed", true).apply()
                
            } catch (e: Exception) {
                // If our error handling crashes, don't prevent the original handler
                Log.e("CrashHandler", "Error handling crash", e)
            }
            
            // Let the default handler deal with the exception
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    private fun logCrashToFile(throwable: Throwable) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
                .format(Date())
            val filename = "crash_$timestamp.txt"
            val file = File(filesDir, filename)
            
            FileOutputStream(file).use { fos ->
                val writer = PrintWriter(fos)
                writer.println("Crash time: $timestamp")
                writer.println("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                writer.println("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}")
                writer.println("\nStack trace:")
                throwable.printStackTrace(writer)
                writer.flush()
            }
        } catch (e: Exception) {
            Log.e("CrashHandler", "Failed to write crash log", e)
        }
    }
    
    private fun setupStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
        
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )
    }
}
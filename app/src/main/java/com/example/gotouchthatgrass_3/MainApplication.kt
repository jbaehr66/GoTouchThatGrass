package com.example.gotouchthatgrass_3

import android.app.Application
import android.os.Build
import android.os.StrictMode
import android.util.Log
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import com.example.gotouchthatgrass_3.ui.theme.FontLoader
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
        
        // Preload fonts asynchronously
        FontLoader.preloadCommonFonts(this)
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
package com.example.gotouchthatgrass_3.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.gotouchthatgrass_3.service.AppBlockerService
import com.example.gotouchthatgrass_3.util.PreferenceManager

/**
 * Broadcast receiver that starts the AppBlockerService when the device boots up
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, starting AppBlockerService")
            
            // Check if app blocking is enabled in preferences
            val preferenceManager = PreferenceManager(context)
            if (preferenceManager.appBlockingEnabled) {
                try {
                    // Start the app blocker service using startForegroundService
                    // as required for Android 8.0+ when starting from background
                    val serviceIntent = Intent(context, AppBlockerService::class.java)
                    
                    // Use startForegroundService to avoid BackgroundServiceStartNotAllowedException
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    
                    Log.d(TAG, "Successfully requested service start")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start service", e)
                }
            } else {
                Log.d(TAG, "App blocking is disabled in preferences")
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
package com.example.gotouchthatgrass_3.util

// util/PreferenceManager.kt

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Main thread getters for UI
    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) {
            // Use commit() for immediate writes but should be called from background
            prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()
        }

    var appBlockingEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_BLOCKING_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_APP_BLOCKING_ENABLED, value).apply()
        }

    var lastChallengeTimestamp: Long
        get() = prefs.getLong(KEY_LAST_CHALLENGE_TIMESTAMP, 0)
        set(value) {
            // Use commit() instead of apply() as it's important to save immediately
            // But need to run on background thread
            Thread {
                prefs.edit().putLong(KEY_LAST_CHALLENGE_TIMESTAMP, value).commit()
            }.start()
        }

    var streak: Int
        get() = prefs.getInt(KEY_STREAK, 0)
        set(value) {
            // Use commit() instead of apply() as it's important to save immediately
            // But need to run on background thread
            Thread {
                prefs.edit().putInt(KEY_STREAK, value).commit()
            }.start()
        }

    var notificationTime: String
        get() = prefs.getString(KEY_NOTIFICATION_TIME, "18:00") ?: "18:00"
        set(value) {
            prefs.edit().putString(KEY_NOTIFICATION_TIME, value).apply()
        }

    // Safe background thread methods
    suspend fun setNotificationsEnabledAsync(value: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()
    }

    suspend fun setAppBlockingEnabledAsync(value: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(KEY_APP_BLOCKING_ENABLED, value).apply()
    }

    suspend fun setLastChallengeTimestampAsync(value: Long) = withContext(Dispatchers.IO) {
        prefs.edit().putLong(KEY_LAST_CHALLENGE_TIMESTAMP, value).apply()
    }

    suspend fun setStreakAsync(value: Int) = withContext(Dispatchers.IO) {
        prefs.edit().putInt(KEY_STREAK, value).apply()
    }

    suspend fun resetStreakAsync() = withContext(Dispatchers.IO) {
        prefs.edit().putInt(KEY_STREAK, 0).apply()
    }

    // For backward compatibility
    fun resetStreak() {
        prefs.edit().putInt(KEY_STREAK, 0).apply()
    }

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_IS_FIRST_LAUNCH, true)
        set(value) {
            prefs.edit().putBoolean(KEY_IS_FIRST_LAUNCH, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "gotouchthatgrass_prefs"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_APP_BLOCKING_ENABLED = "app_blocking_enabled"
        private const val KEY_LAST_CHALLENGE_TIMESTAMP = "last_challenge_timestamp"
        private const val KEY_STREAK = "streak"
        private const val KEY_NOTIFICATION_TIME = "notification_time"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
    }
}
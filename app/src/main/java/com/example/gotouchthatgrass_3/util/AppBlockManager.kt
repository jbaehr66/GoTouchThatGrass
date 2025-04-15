package com.example.gotouchthatgrass_3.util

// util/AppBlockManager.kt

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import android.util.Log
import com.example.gotouchthatgrass_3.MainActivity
import com.example.gotouchthatgrass_3.db.AppDatabase
import com.example.gotouchthatgrass_3.models.BlockedApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class AppBlockManager(private val context: Context) {
    private val appDatabase = AppDatabase.getDatabase(context)
    private val blockedAppDao = appDatabase.blockedAppDao()
    private val preferenceManager = PreferenceManager(context)

    /**
     * Adds an app to the block list
     * 
     * @param packageName The package name of the app to block
     * @param appName The display name of the app
     * @return true if the app was successfully added, false otherwise
     */
    suspend fun addAppToBlockList(packageName: String, appName: String): Boolean {
        return try {
            val blockedApp = BlockedApp(
                packageName = packageName, 
                appName = appName,
                isCurrentlyBlocked = true,
                blockStartTime = System.currentTimeMillis()
            )
            val insertId = withContext(Dispatchers.IO) {
                blockedAppDao.insert(blockedApp)
            }
            val success = insertId > 0
            if (success) {
                Log.d("AppBlockManager", "Successfully added $appName to block list")
            } else {
                Log.e("AppBlockManager", "Failed to add $appName to block list")
            }
            success
        } catch (e: Exception) {
            Log.e("AppBlockManager", "Error adding app to block list: $packageName", e)
            false
        }
    }

    /**
     * Removes an app from the block list
     * 
     * @param packageName The package name of the app to unblock
     * @return true if the app was successfully removed, false otherwise
     */
    suspend fun removeAppFromBlockList(packageName: String): Boolean {
        return try {
            val app = withContext(Dispatchers.IO) {
                blockedAppDao.getAppByPackageName(packageName)
            }
            
            if (app != null) {
                val deleteCount = withContext(Dispatchers.IO) {
                    blockedAppDao.delete(app)
                }
                val success = deleteCount > 0
                if (success) {
                    Log.d("AppBlockManager", "Successfully removed $packageName from block list")
                } else {
                    Log.e("AppBlockManager", "Failed to remove $packageName from block list")
                }
                success
            } else {
                Log.w("AppBlockManager", "App not found in block list: $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e("AppBlockManager", "Error removing app from block list: $packageName", e)
            false
        }
    }

    /**
     * Unblocks all apps by setting isCurrentlyBlocked to false
     * 
     * @return The number of apps that were unblocked
     */
    suspend fun unblockAllApps(): Int {
        return try {
            val count = withContext(Dispatchers.IO) {
                blockedAppDao.unblockAllApps()
            }
            Log.d("AppBlockManager", "Unblocked $count apps")
            count
        } catch (e: Exception) {
            Log.e("AppBlockManager", "Error unblocking all apps", e)
            0
        }
    }

    /**
     * Checks if an app is currently blocked
     * 
     * @param packageName The package name to check
     * @return true if the app is blocked, false otherwise
     */
    suspend fun isAppBlocked(packageName: String): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val app = blockedAppDao.getAppByPackageName(packageName)
                val isBlocked = app?.isCurrentlyBlocked ?: false
                Log.d("AppBlockManager", "Checked if app is blocked: $packageName, result: $isBlocked")
                isBlocked
            }
        } catch (e: Exception) {
            Log.e("AppBlockManager", "Error checking if app is blocked: $packageName", e)
            false
        }
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(), context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun getCurrentForegroundApp(): String? {
        if (!hasUsageStatsPermission()) {
            return null
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - TimeUnit.MINUTES.toMillis(5),
            time
        )

        if (stats.isEmpty()) {
            return null
        }

        // Find the last used app
        return stats.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    fun launchMainActivity() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}
package com.example.gotouchthatgrass_3.ui.blocked

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.gotouchthatgrass_3.db.AppDatabase
import com.example.gotouchthatgrass_3.models.BlockedApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BlockedAppsViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getDatabase(application)
    private val blockedAppDao = appDatabase.blockedAppDao()
    
    // Track operations in progress to prevent duplicates
    private val operationsInProgress = HashSet<String>()
    
    // For synchronizing access to the operations tracking set
    private val lock = Any()

    // LiveData of all blocked apps
    val blockedApps: LiveData<List<BlockedApp>> = blockedAppDao.getAllBlockedApps()


    /**
     * Adds an app to the block list
     *
     * @param packageName The package name of the app to block
     * @param appName The display name of the app
     */
    fun blockApp(packageName: String, appName: String) {
        // Use operation tracking to prevent duplicate operations
        val operationKey = "block:$packageName"
        
        synchronized(lock) {
            if (operationsInProgress.contains(operationKey)) {
                Log.d("BlockedAppsViewModel", "Ignoring duplicate block request for $packageName")
                return
            }
            operationsInProgress.add(operationKey)
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("BlockedAppsViewModel", "Blocking app: $appName ($packageName)")
                
                // First check if the app is already blocked
                val existingApp = blockedAppDao.getAppByPackageName(packageName)
                if (existingApp != null && existingApp.isCurrentlyBlocked) {
                    Log.d("BlockedAppsViewModel", "App $packageName is already blocked, skipping")
                    return@launch
                }
                
                val blockedApp = BlockedApp(
                    packageName = packageName,
                    appName = appName,
                    isCurrentlyBlocked = true,
                    blockStartTime = System.currentTimeMillis()
                )
                
                // Using non-suspend function but on IO thread
                val insertId = blockedAppDao.insert(blockedApp)
                Log.d("BlockedAppsViewModel", "App blocked successfully with ID: $insertId")
            } catch (e: Exception) {
                Log.e("BlockedAppsViewModel", "Error blocking app: $packageName", e)
            } finally {
                // Always remove from operations in progress when done
                synchronized(lock) {
                    operationsInProgress.remove(operationKey)
                }
            }
        }
    }

    /**
     * Removes an app from the block list
     *
     * @param packageName The package name of the app to unblock
     */
    fun unblockApp(packageName: String) {
        // Use operation tracking to prevent duplicate operations
        val operationKey = "unblock:$packageName"
        
        synchronized(lock) {
            if (operationsInProgress.contains(operationKey)) {
                Log.d("BlockedAppsViewModel", "Ignoring duplicate unblock request for $packageName")
                return
            }
            operationsInProgress.add(operationKey)
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("BlockedAppsViewModel", "Unblocking app: $packageName")
                
                // Get the app (using non-suspend function but on IO thread)
                val app = blockedAppDao.getAppByPackageName(packageName)
                
                if (app != null) {
                    // Skip if already unblocked
                    if (!app.isCurrentlyBlocked) {
                        Log.d("BlockedAppsViewModel", "App $packageName is already unblocked, skipping")
                        return@launch
                    }
                    
                    val updatedApp = app.copy(isCurrentlyBlocked = false)
                    
                    // Then update it (using non-suspend function)
                    val updateCount = blockedAppDao.update(updatedApp)
                    Log.d("BlockedAppsViewModel", "App unblocked successfully: $updateCount records updated")
                } else {
                    Log.w("BlockedAppsViewModel", "App not found to unblock: $packageName")
                }
            } catch (e: Exception) {
                Log.e("BlockedAppsViewModel", "Error unblocking app: $packageName", e)
            } finally {
                // Always remove from operations in progress when done
                synchronized(lock) {
                    operationsInProgress.remove(operationKey)
                }
            }
        }
    }

    /**
     * Unblocks all apps in the block list
     */
    fun unblockAllApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("BlockedAppsViewModel", "Unblocking all apps")
                
                // Execute the unblock operation on the IO thread (non-suspend method)
                val rowsUpdated = withContext(Dispatchers.IO) {
                    blockedAppDao.unblockAllApps()
                }
                
                Log.d("BlockedAppsViewModel", "Unblocked all apps successfully: $rowsUpdated rows updated")
            } catch (e: Exception) {
                Log.e("BlockedAppsViewModel", "Error unblocking all apps", e)
            }
        }
    }

    /**
     * Checks if an app is currently blocked
     * Still marked as suspend for convenience in calling code, but uses non-suspend DAO methods
     *
     * @param packageName The package name to check
     * @return true if the app is blocked, false otherwise
     */
    suspend fun isAppBlocked(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Using non-suspend function but on IO thread
            val app = blockedAppDao.getAppByPackageName(packageName)
            val isBlocked = app?.isCurrentlyBlocked == true
            Log.d("BlockedAppsViewModel", "Checked if app is blocked: $packageName, result: $isBlocked")
            isBlocked
        } catch (e: Exception) {
            Log.e("BlockedAppsViewModel", "Error checking if app is blocked: $packageName", e)
            false
        }
    }
}
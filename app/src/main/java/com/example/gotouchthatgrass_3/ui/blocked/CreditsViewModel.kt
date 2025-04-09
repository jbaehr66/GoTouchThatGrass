package com.example.gotouchthatgrass_3.ui.blocked

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.gotouchthatgrass_3.db.AppDatabase
import com.example.gotouchthatgrass_3.models.UserCredits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for managing user credits used to unblock apps.
 */
class CreditsViewModel(application: Application) : AndroidViewModel(application) {

    private val appDatabase = AppDatabase.getDatabase(application)
    private val userCreditsDao = appDatabase.userCreditsDao()
    private val blockedAppDao = appDatabase.blockedAppDao()
    
    // LiveData of user credits
    val userCredits: LiveData<UserCredits?> = userCreditsDao.getUserCredits()
    
    // Available credits as a formatted string
    val availableCreditsText = userCredits.map { credits ->
        "Credits: ${credits?.availableCredits ?: 0}"
    }
    
    // Cost map for unblocking apps (cached values)
    private val unblockCosts = HashMap<String, Int>()
    
    init {
        // Initialize credits if needed
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userCreditsDao.initializeIfNeeded()
            } catch (e: Exception) {
                Log.e("CreditsViewModel", "Failed to initialize credits", e)
            }
        }
    }
    
    /**
     * Calculate the cost of unblocking an app (based on how long it's been blocked)
     */
    fun getUnblockCost(packageName: String): LiveData<Int> {
        val cost = MutableLiveData<Int>()
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = blockedAppDao.getAppByPackageName(packageName)
                if (app != null && app.isCurrentlyBlocked) {
                    // Calculate cost based on how long the app has been blocked
                    // Base cost is 1 credit, plus 1 credit for each day it's been blocked (max 5)
                    val blockDurationHours = (System.currentTimeMillis() - app.blockStartTime) / (1000 * 60 * 60)
                    val calculatedCost = 1 + (blockDurationHours / 24).coerceAtMost(4)
                    
                    // Cache the cost
                    unblockCosts[packageName] = calculatedCost.toInt()
                    
                    withContext(Dispatchers.Main) {
                        cost.value = calculatedCost.toInt()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        cost.value = 0
                    }
                }
            } catch (e: Exception) {
                Log.e("CreditsViewModel", "Error calculating unblock cost", e)
                withContext(Dispatchers.Main) {
                    cost.value = 0
                }
            }
        }
        
        return cost
    }
    
    /**
     * Check if the user can afford to unblock an app
     */
    fun canAffordUnblock(packageName: String): LiveData<Boolean> {
        val canAfford = MutableLiveData<Boolean>()
        
        viewModelScope.launch {
            try {
                // Get the cost of unblocking this app
                val costLiveData = getUnblockCost(packageName)
                val cost = costLiveData.value ?: 1
                
                // Get available credits
                val availableCredits = userCredits.value?.availableCredits ?: 0
                
                // Check if user can afford it
                canAfford.value = availableCredits >= cost
            } catch (e: Exception) {
                Log.e("CreditsViewModel", "Error checking if can afford unblock", e)
                canAfford.value = false
            }
        }
        
        return canAfford
    }
    
    /**
     * Check if the user can afford to unblock all apps
     */
    fun canAffordUnblockAll(): LiveData<Boolean> {
        val canAfford = MutableLiveData<Boolean>()
        
        viewModelScope.launch {
            try {
                val totalCost = getTotalUnblockCost()
                val availableCredits = userCredits.value?.availableCredits ?: 0
                
                canAfford.value = availableCredits >= totalCost
            } catch (e: Exception) {
                Log.e("CreditsViewModel", "Error checking if can afford unblock all", e)
                canAfford.value = false
            }
        }
        
        return canAfford
    }
    
    /**
     * Calculate the total cost to unblock all apps
     */
    private suspend fun getTotalUnblockCost(): Int {
        return withContext(Dispatchers.IO) {
            try {
                val blockedApps = blockedAppDao.getAllBlockedApps().value?.filter { it.isCurrentlyBlocked } ?: emptyList()
                
                var totalCost = 0
                for (app in blockedApps) {
                    val cost = unblockCosts[app.packageName] ?: 1
                    totalCost += cost
                }
                
                totalCost
            } catch (e: Exception) {
                Log.e("CreditsViewModel", "Error calculating total unblock cost", e)
                0
            }
        }
    }
    
    /**
     * Spend credits to unblock an app
     */
    fun spendCreditsForUnblock(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cost = unblockCosts[packageName] ?: 1
                userCreditsDao.spendCredits(cost)
                Log.d("CreditsViewModel", "Spent $cost credits to unblock $packageName")
            } catch (e: Exception) {
                Log.e("CreditsViewModel", "Error spending credits for unblock", e)
            }
        }
    }
    
    /**
     * Spend credits to unblock all apps
     */
    fun spendCreditsForUnblockAll() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val totalCost = getTotalUnblockCost()
                userCreditsDao.spendCredits(totalCost)
                Log.d("CreditsViewModel", "Spent $totalCost credits to unblock all apps")
            } catch (e: Exception) {
                Log.e("CreditsViewModel", "Error spending credits for unblock all", e)
            }
        }
    }
    
    /**
     * Add credits (earned through challenges)
     */
    fun addCredits(amount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userCreditsDao.addCredits(amount)
                Log.d("CreditsViewModel", "Added $amount credits")
            } catch (e: Exception) {
                Log.e("CreditsViewModel", "Error adding credits", e)
            }
        }
    }
}
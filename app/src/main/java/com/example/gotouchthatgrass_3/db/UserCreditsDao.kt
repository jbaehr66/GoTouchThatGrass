package com.example.gotouchthatgrass_3.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.gotouchthatgrass_3.models.UserCredits

/**
 * Data Access Object for user credits.
 */
@Dao
interface UserCreditsDao {
    @Query("SELECT * FROM user_credits WHERE id = 1")
    fun getUserCredits(): LiveData<UserCredits?>
    
    @Query("SELECT * FROM user_credits WHERE id = 1")
    fun getUserCreditsSync(): UserCredits?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(userCredits: UserCredits): Long
    
    @Update
    fun update(userCredits: UserCredits): Int
    
    @Query("UPDATE user_credits SET availableCredits = availableCredits + :amount, lifetimeCreditsEarned = lifetimeCreditsEarned + :amount, lastUpdated = :timestamp WHERE id = 1")
    fun addCredits(amount: Int, timestamp: Long = System.currentTimeMillis()): Int
    
    @Query("UPDATE user_credits SET availableCredits = availableCredits - :amount, lifetimeCreditsSpent = lifetimeCreditsSpent + :amount, lastUpdated = :timestamp WHERE id = 1 AND availableCredits >= :amount")
    fun spendCredits(amount: Int, timestamp: Long = System.currentTimeMillis()): Int
    
    @Query("SELECT availableCredits FROM user_credits WHERE id = 1")
    fun getAvailableCredits(): Int?
    
    @Transaction
    fun initializeIfNeeded() {
        val existing = getUserCreditsSync()
        if (existing == null) {
            insert(UserCredits(availableCredits = 5)) // Start with 5 credits
        }
    }
}
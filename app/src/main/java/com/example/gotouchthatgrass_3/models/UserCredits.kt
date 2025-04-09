package com.example.gotouchthatgrass_3.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Model representing user credits, which can be earned through challenges
 * and spent to unblock apps.
 */
@Entity(tableName = "user_credits")
data class UserCredits(
    @PrimaryKey
    var id: Int = 1, // Single row for the current user
    var availableCredits: Int = 0,
    var lifetimeCreditsEarned: Int = 0,
    var lifetimeCreditsSpent: Int = 0,
    var lastUpdated: Long = System.currentTimeMillis()
)
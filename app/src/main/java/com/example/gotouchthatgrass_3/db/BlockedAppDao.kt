package com.example.gotouchthatgrass_3.db

// db/BlockedAppDao.kt
import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.gotouchthatgrass_3.models.BlockedApp

/**
 * Data Access Object for BlockedApp entities
 * Using non-suspend functions to avoid Room compilation issues
 */
@Dao
interface BlockedAppDao {
    /**
     * Get all blocked apps as LiveData
     */
    @Query("SELECT * FROM blocked_apps")
    fun getAllBlockedApps(): LiveData<List<BlockedApp>>

    /**
     * Get only currently blocked apps as LiveData
     */
    @Query("SELECT * FROM blocked_apps WHERE isCurrentlyBlocked = 1")
    fun getCurrentlyBlockedApps(): LiveData<List<BlockedApp>>

    /**
     * Insert a new blocked app
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(app: BlockedApp): Long

    /**
     * Update an existing blocked app
     */
    @Update
    fun update(app: BlockedApp): Int

    /**
     * Delete a blocked app
     */
    @Delete
    fun delete(app: BlockedApp): Int

    /**
     * Unblock all apps by setting isCurrentlyBlocked = 0
     */
    @Query("UPDATE blocked_apps SET isCurrentlyBlocked = 0 WHERE isCurrentlyBlocked = 1")
    fun unblockAllApps(): Int

    /**
     * Get a single app by package name
     */
    @Query("SELECT * FROM blocked_apps WHERE packageName = :pkgName")
    fun getAppByPackageName(pkgName: String): BlockedApp?
}
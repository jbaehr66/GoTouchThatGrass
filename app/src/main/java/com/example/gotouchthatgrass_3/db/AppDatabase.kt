package com.example.gotouchthatgrass_3.db

// db/AppDatabase.kt

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gotouchthatgrass_3.models.BlockedApp
import com.example.gotouchthatgrass_3.models.Challenge
import com.example.gotouchthatgrass_3.models.UserCredits
import com.example.gotouchthatgrass_3.util.Converters

@Database(
    entities = [BlockedApp::class, Challenge::class, UserCredits::class], 
    version = 3, 
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun userCreditsDao(): UserCreditsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "gotouchthatgrass_database"
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // Add migrations for version changes
                    .fallbackToDestructiveMigration() // Fallback option for development
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            Log.d("AppDatabase", "Database created successfully")
                        }
                        
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            Log.d("AppDatabase", "Database opened successfully")
                        }
                    })
                    .build()
                    
                    // Initialize the user credits if needed
                    try {
                        instance.userCreditsDao().initializeIfNeeded()
                    } catch (e: Exception) {
                        Log.e("AppDatabase", "Failed to initialize credits", e)
                    }
                    
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    // Log the error
                    Log.e("AppDatabase", "Error creating database", e)
                    
                    // Reset database as last resort (will lose data but prevent crash)
                    context.deleteDatabase("gotouchthatgrass_database")
                    
                    // Create a fresh database
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "gotouchthatgrass_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                    
                    // Initialize the user credits
                    try {
                        instance.userCreditsDao().initializeIfNeeded()
                    } catch (ex: Exception) {
                        Log.e("AppDatabase", "Failed to initialize credits after reset", ex)
                    }
                    
                    INSTANCE = instance
                    instance
                }
            }
        }
        
        // Older migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Previously defined migration
            }
        }
        
        // Migration from version 2 to 3 (adding the UserCredits table)
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `user_credits` " +
                    "(`id` INTEGER NOT NULL, " +
                    "`availableCredits` INTEGER NOT NULL, " +
                    "`lifetimeCreditsEarned` INTEGER NOT NULL, " +
                    "`lifetimeCreditsSpent` INTEGER NOT NULL, " +
                    "`lastUpdated` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))"
                )
                
                // Insert initial credits record
                database.execSQL(
                    "INSERT INTO `user_credits` (`id`, `availableCredits`, `lifetimeCreditsEarned`, " +
                    "`lifetimeCreditsSpent`, `lastUpdated`) VALUES (1, 5, 5, 0, " + System.currentTimeMillis() + ")"
                )
            }
        }
    }
}
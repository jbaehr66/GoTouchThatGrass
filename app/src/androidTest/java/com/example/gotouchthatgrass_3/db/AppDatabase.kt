package com.example.gotouchthatgrass_3.db

// db/AppDatabase.kt

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gotouchthatgrass_3.models.BlockedApp
import com.example.gotouchthatgrass_3.models.Challenge
import com.example.gotouchthatgrass_3.util.Converters

@Database(entities = [BlockedApp::class, Challenge::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun challengeDao(): ChallengeDao

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
                    .addMigrations(MIGRATION_1_2) // Add migrations for future version changes
                    .fallbackToDestructiveMigration() // Fallback option for development
                    .build()
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    // Log the error
                    e.printStackTrace()
                    
                    // Reset database as last resort (will lose data but prevent crash)
                    context.deleteDatabase("gotouchthatgrass_database")
                    
                    // Create a fresh database
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "gotouchthatgrass_database"
                    ).build()
                    INSTANCE = instance
                    instance
                }
            }
        }
        
        // Define migrations for version changes
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // If we need to update schema in the future, add migration logic here
                // Example: database.execSQL("ALTER TABLE challenges ADD COLUMN location TEXT DEFAULT ''")
            }
        }
    }
}
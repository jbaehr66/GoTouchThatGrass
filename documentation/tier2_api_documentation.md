# Go Touch That Grass - API Documentation

This document provides detailed API-level documentation for the core components of the Go Touch That Grass application, including method signatures, descriptions, and usage examples.

## Database Layer

### AppDatabase

The central database configuration using Room persistence library.

```kotlin
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
}
```

### BlockedAppDao

Data Access Object for managing blocked applications.

```kotlin
@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_apps ORDER BY appName ASC")
    fun getAllApps(): Flow<List<BlockedApp>>

    @Query("SELECT * FROM blocked_apps WHERE isCurrentlyBlocked = 1 ORDER BY appName ASC")
    fun getCurrentlyBlockedApps(): Flow<List<BlockedApp>>

    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName")
    suspend fun getAppByPackageName(packageName: String): BlockedApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(blockedApp: BlockedApp)

    @Delete
    suspend fun delete(blockedApp: BlockedApp)

    @Query("UPDATE blocked_apps SET isCurrentlyBlocked = :isBlocked WHERE packageName = :packageName")
    suspend fun updateBlockStatus(packageName: String, isBlocked: Boolean)

    @Query("UPDATE blocked_apps SET isCurrentlyBlocked = 0")
    suspend fun unblockAllApps()
}
```

### ChallengeDao

Data Access Object for managing challenge records.

```kotlin
@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenges ORDER BY timestamp DESC")
    fun getAllChallenges(): Flow<List<Challenge>>

    @Query("SELECT * FROM challenges WHERE isSuccessful = 1 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastSuccessfulChallenge(): Challenge?

    @Query("SELECT COUNT(*) FROM challenges WHERE isSuccessful = 1")
    fun getSuccessfulChallengeCount(): Flow<Int>

    @Insert
    suspend fun insert(challenge: Challenge)

    @Delete
    suspend fun delete(challenge: Challenge)
}
```

## Utility Layer

### GrassDetector

Handles grass detection through both color analysis and machine learning.

```kotlin
class GrassDetector(private val context: Context) {
    // Check if image has significant green content
    fun hasGreenColor(bitmap: Bitmap, threshold: Float = 0.3f): Boolean

    // Use ML Kit to identify grass in images
    suspend fun detectGrassUsingML(imageBitmap: Bitmap): Boolean

    // Combined approach using both methods
    suspend fun isGrassInImage(imageBitmap: Bitmap): Boolean
}
```

**Usage Example:**
```kotlin
// In GrassDetectionActivity
lifecycleScope.launch {
    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
    val containsGrass = grassDetector.isGrassInImage(bitmap)
    
    if (containsGrass) {
        // Handle successful grass detection
        saveChallenge(photoFile.absolutePath, true)
        appBlockManager.unblockAllApps()
        updateStreak()
    } else {
        // Handle failed detection
        showRetryOptions()
    }
}
```

### AppBlockManager

Manages app blocking functionality and usage stats permissions.

```kotlin
class AppBlockManager(private val context: Context) {
    // Check if usage stats permission is granted
    fun hasUsageStatsPermission(): Boolean

    // Get the current foreground app package name
    fun getCurrentForegroundApp(): String?

    // Block a specific app
    suspend fun blockApp(packageName: String, appName: String)

    // Unblock a specific app
    suspend fun unblockApp(packageName: String)

    // Unblock all apps
    suspend fun unblockAllApps()

    // Get a list of all installed apps
    fun getInstalledApps(): List<AppInfo>
}
```

**Usage Example:**
```kotlin
// In BlockedAppsViewModel
viewModelScope.launch {
    // Block a selected app
    appBlockManager.blockApp(packageName, appName)
    
    // Later, after a challenge is completed
    appBlockManager.unblockAllApps()
}
```

### PreferenceManager

Manages user preferences and streak data.

```kotlin
class PreferenceManager(context: Context) {
    // Streak management
    var streak: Int
    var lastChallengeTimestamp: Long
    
    // Settings
    var notificationsEnabled: Boolean
    var dailyReminderTime: Long
    var autoResetStreakEnabled: Boolean
}
```

**Usage Example:**
```kotlin
// After completing a grass detection challenge
private fun updateStreak() {
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_YEAR)
    
    val lastChallengeDay = getLastChallengeDay()
    
    // Update last challenge timestamp
    preferenceManager.lastChallengeTimestamp = System.currentTimeMillis()
    
    // If this is a consecutive day, increment streak
    if (isConsecutiveDay(today, lastChallengeDay)) {
        val newStreak = preferenceManager.streak + 1
        preferenceManager.streak = newStreak
        
        // Check for milestone
        if (newStreak > 0 && newStreak % 7 == 0) {
            NotificationHelper(this).sendStreakMilestoneNotification(newStreak)
        }
    } else {
        // Reset streak if not consecutive
        preferenceManager.streak = 1
    }
}
```

## Service Layer

### AppBlockerService

Foreground service that monitors app usage and enforces blocking.

```kotlin
class AppBlockerService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    
    private fun startAppMonitoring()
    
    private suspend fun hasCompletedChallengeToday(): Boolean
}
```

**Usage Example:**
```kotlin
// Starting the service
val serviceIntent = Intent(context, AppBlockerService::class.java)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    context.startForegroundService(serviceIntent)
} else {
    context.startService(serviceIntent)
}
```

## ViewModel Layer

### BlockedAppsViewModel

Manages the list of blocked applications.

```kotlin
class BlockedAppsViewModel(application: Application) : AndroidViewModel(application) {
    // Get all installed apps
    fun getInstalledApps(): List<AppInfo>
    
    // Get all blocked apps
    fun getBlockedApps(): LiveData<List<BlockedApp>>
    
    // Block an app
    fun blockApp(packageName: String, appName: String)
    
    // Unblock an app
    fun unblockApp(packageName: String)
}
```

### HomeViewModel

Manages the home screen data.

```kotlin
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    // Get blocked apps for display
    fun getBlockedApps(): LiveData<List<BlockedApp>>
    
    // Get challenge history
    fun getChallengeHistory(): LiveData<List<Challenge>>
    
    // Get streak information
    fun getCurrentStreak(): Int
}
```

## Common Patterns

### Permission Handling

```kotlin
// Check and request usage stats permission
private fun checkUsageStatsPermission() {
    if (!appBlockManager.hasUsageStatsPermission()) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }
}
```

### Image Capture and Processing

```kotlin
// Take photo using CameraX
private fun takePhoto() {
    val imageCapture = imageCapture ?: return
    
    val photoFile = File(
        getOutputDirectory(),
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US)
            .format(System.currentTimeMillis()) + ".jpg"
    )
    
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(this),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                processCapturedImage(photoFile)
            }
            
            override fun onError(exception: ImageCaptureException) {
                // Handle error
            }
        }
    )
}
```

### Streak Calculation

```kotlin
// Check if two dates are consecutive days
private fun areConsecutiveDays(current: Calendar, previous: Calendar): Boolean {
    // Same year, consecutive days
    if (current.get(Calendar.YEAR) == previous.get(Calendar.YEAR)) {
        return current.get(Calendar.DAY_OF_YEAR) - previous.get(Calendar.DAY_OF_YEAR) == 1
    }
    
    // Year boundary case (Dec 31 -> Jan 1)
    return previous.get(Calendar.MONTH) == Calendar.DECEMBER &&
           previous.get(Calendar.DAY_OF_MONTH) == 31 &&
           current.get(Calendar.MONTH) == Calendar.JANUARY &&
           current.get(Calendar.DAY_OF_MONTH) == 1
}
```
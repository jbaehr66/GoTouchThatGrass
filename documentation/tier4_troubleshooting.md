# Go Touch That Grass - Crash Analysis & Troubleshooting

This document identifies potential crash points in the Go Touch That Grass application and provides detailed fixes for startup and runtime issues.

## Startup Crash Analysis

### Common Crash Points

| Component | Crash Trigger | Root Cause | Severity |
|-----------|---------------|------------|----------|
| AppDatabase | Initialization | Schema version mismatch or migration failure | Critical |
| AppBlockerService | Foreground service start | Missing notification channel or improper setup | Critical |
| PreferenceManager | First access | SharedPreferences corruption | High |
| GrassDetector | ML Kit initialization | Missing ML Kit dependencies | High |
| Boot Receiver | System boot | Missing BOOT_COMPLETED permission | Medium |

### Critical Startup Fix: Database Initialization

**Issue:** The most common startup crash occurs in `AppDatabase.getDatabase()` when there's a schema mismatch between the database version on disk and the expected version in code.

**Error Signature:**
```
Fatal Exception: java.lang.RuntimeException: Unable to create application com.example.gotouchthatgrass_3.MainApplication: androidx.room.MissingMigrationException: Database version mismatch.
Expected: 1, found: 2
```

**Fix Implementation:**

```kotlin
// In AppDatabase.kt, modify getDatabase() to handle migration:
fun getDatabase(context: Context): AppDatabase {
    return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "gotouchthatgrass_database"
        )
        .addMigrations(MIGRATION_1_2) // Add migrations
        .fallbackToDestructiveMigration() // Fallback for development only
        .build()
        INSTANCE = instance
        instance
    }
}

// Define migrations for version changes
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // If schema changed, handle migration logic
        // Example: database.execSQL("ALTER TABLE challenges ADD COLUMN location TEXT DEFAULT ''")
    }
}
```

**Preventative Measure:** Add a try-catch block to handle database errors gracefully:

```kotlin
try {
    appDatabase = AppDatabase.getDatabase(this)
} catch (e: Exception) {
    // Log the error
    Log.e("MainActivity", "Database initialization failed", e)
    
    // Reset database as last resort (will lose data but prevent crash)
    context.deleteDatabase("gotouchthatgrass_database")
    appDatabase = AppDatabase.getDatabase(this)
    
    // Show error toast
    Toast.makeText(this, "Database reset due to error", Toast.LENGTH_LONG).show()
}
```

### Critical Startup Fix: Foreground Service

**Issue:** On Android 8.0+, starting a foreground service without proper notification channel setup causes immediate crash.

**Error Signature:**
```
Fatal Exception: android.app.RemoteServiceException: 
Context.startForegroundService() did not then call Service.startForeground()
```

**Fix Implementation:**

```kotlin
// In AppBlockerService.kt, fix onStartCommand:
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.d(TAG, "Service started")

    try {
        // Create foreground notification
        val notification = createForegroundNotification()
        
        // Must call startForeground within 5 seconds of service start
        startForeground(NOTIFICATION_ID, notification)
        
        // Start monitoring apps after ensuring foreground status
        startAppMonitoring()
    } catch (Exception e) {
        Log.e(TAG, "Failed to start service in foreground", e)
        stopSelf() // Stop service if we can't run in foreground
    }

    return START_STICKY
}

// Ensure notification channel exists
private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Blocker Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background service that monitors app usage"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create notification channel", e)
        }
    }
}
```

**Checking for Missing Icon Resource:** Line 172-173 in AppBlockerService.kt has the notification icon commented out, which could cause issues:

```kotlin
// Original problematic code
// .setSmallIcon(R.drawable.ic_notification)

// Fix by using an existing icon or fallback to app icon
.setSmallIcon(
    try {
        R.drawable.ic_notification 
    } catch (e: Exception) {
        R.drawable.ic_launcher_foreground // Fallback icon
    }
)
```

## Runtime Crash Analysis

### GrassDetectionActivity Crashes

**Issue 1:** NullPointerException in `GrassDetectionActivity.verifyGrassAndUnblock()` when photo file is null.

**Fix:**
```kotlin
private fun verifyGrassAndUnblock() {
    val photoFile = this@GrassDetectionActivity.photoFile
    if (photoFile == null || !photoFile.exists()) {
        Toast.makeText(this, "Error: Photo file not found", Toast.LENGTH_SHORT).show()
        binding.instructionText.text = "Please try taking a photo again"
        binding.progressBar.visibility = View.GONE
        binding.confirmButton.isEnabled = true
        binding.retryButton.isEnabled = true
        return
    }
    
    // Proceed with verification...
}
```

**Issue 2:** OutOfMemoryError when loading large images for processing.

**Fix:** Add bitmap sampling to reduce memory usage:

```kotlin
private fun decodeSampledBitmap(photoFile: File, reqWidth: Int, reqHeight: Int): Bitmap {
    // First decode with inJustDecodeBounds=true to check dimensions
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(photoFile.absolutePath, options)

    // Calculate inSampleSize
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

    // Decode bitmap with inSampleSize set
    options.inJustDecodeBounds = false
    return BitmapFactory.decodeFile(photoFile.absolutePath, options)
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    // Raw height and width of image
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}

// Use in the verification method:
val bitmap = decodeSampledBitmap(photoFile, 1024, 1024)
```

### AppBlockerService Monitoring Crashes

**Issue:** The app monitoring coroutine in `AppBlockerService` crashes when accessing `appDatabase` or due to permission issues.

**Error Signature:**
```
Fatal Exception: java.lang.IllegalStateException: 
Room cannot verify the data integrity. Looks like you've changed schema but forgot to update the version number.
```

**Fix:** Add robust error handling to the monitoring job:

```kotlin
private fun startAppMonitoring() {
    // Cancel any existing monitoring job
    monitoringJob?.cancel()

    // Start a new monitoring job with error handling
    monitoringJob = serviceScope.launch {
        while (isActive) {
            try {
                // Check if usage stats permission is granted
                if (!appBlockManager.hasUsageStatsPermission()) {
                    Log.w(TAG, "Usage stats permission not granted")
                    delay(MONITOR_INTERVAL)
                    continue
                }

                // Fix self-package check (line 78)
                val foregroundApp = appBlockManager.getCurrentForegroundApp()
                foregroundApp?.let { packageName ->
                    // Skip our own app - FIXED
                    if (packageName == context.packageName) {
                        delay(MONITOR_INTERVAL)
                        return@let
                    }
                    
                    try {
                        // Check if the app is in our block list - with transaction safety
                        val blockedAppDao = appDatabase.blockedAppDao()
                        val blockedApp = withContext(Dispatchers.IO) {
                            blockedAppDao.getAppByPackageName(packageName)
                        }

                        // Rest of monitoring logic...
                    } catch (dbException: Exception) {
                        Log.e(TAG, "Database error in app monitoring", dbException)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in app monitoring: ${e.message}", e)
            }

            // Check every few seconds
            delay(MONITOR_INTERVAL)
        }
    }
}
```

## Permission-Related Crashes

### Missing Usage Stats Permission

**Issue:** The app crashes when trying to monitor foreground apps without usage stats permission.

**Fix:** Add a comprehensive permission check at startup:

```kotlin
// In MainActivity.onCreate():
private fun checkRequiredPermissions() {
    val missingPermissions = mutableListOf<String>()
    
    // Check usage stats permission
    if (!appBlockManager.hasUsageStatsPermission()) {
        // Cannot request programmatically, need to guide user
        showUsageStatsPermissionDialog()
    }
    
    // Check notification permission for Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
            != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    // Check camera permission
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
        != PackageManager.PERMISSION_GRANTED) {
        missingPermissions.add(Manifest.permission.CAMERA)
    }
    
    // Request permissions if needed
    if (missingPermissions.isNotEmpty()) {
        ActivityCompat.requestPermissions(
            this,
            missingPermissions.toTypedArray(),
            PERMISSION_REQUEST_CODE
        )
    }
}

private fun showUsageStatsPermissionDialog() {
    AlertDialog.Builder(this)
        .setTitle("Permission Required")
        .setMessage("Go Touch That Grass needs access to usage data to monitor blocked apps.")
        .setPositiveButton("Grant Permission") { _, _ ->
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        }
        .setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
            Toast.makeText(this, "App blocking will not work without this permission", 
                          Toast.LENGTH_LONG).show()
        }
        .setCancelable(false)
        .show()
}
```

## ML Kit Initialization Crashes

**Issue:** ML Kit initialization fails due to missing dependencies or incompatible devices.

**Fix:** Add graceful fallback for ML Kit failures:

```kotlin
// In GrassDetector.kt
class GrassDetector(private val context: Context) {
    private var mlKitAvailable = true
    
    init {
        // Check if ML Kit is available
        try {
            ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        } catch (e: Exception) {
            Log.w("GrassDetector", "ML Kit not available: ${e.message}")
            mlKitAvailable = false
        }
    }
    
    suspend fun isGrassInImage(imageBitmap: Bitmap): Boolean {
        // Always check with basic color detection
        val hasGreenDominance = hasGreenColor(imageBitmap, threshold = 0.25f)
        
        // Only try ML detection if available
        val mlDetection = if (mlKitAvailable) {
            try {
                detectGrassUsingML(imageBitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        } else false
        
        return hasGreenDominance || mlDetection
    }
}
```

## Global Error Handler

Add a global error handler to catch unexpected exceptions and provide better diagnostics:

```kotlin
// In MainApplication.kt
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Set up global error handler
        setupExceptionHandler()
    }
    
    private fun setupExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // Log crash details to a file
                logCrashToFile(throwable)
                
                // Show a friendly crash notification next time app opens
                val preferences = getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
                preferences.edit().putBoolean("has_crashed", true).apply()
                
            } catch (e: Exception) {
                // If our error handling crashes, don't prevent the original handler
                Log.e("CrashHandler", "Error handling crash", e)
            }
            
            // Let the default handler deal with the exception
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    private fun logCrashToFile(throwable: Throwable) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
                .format(Date())
            val filename = "crash_$timestamp.txt"
            val file = File(filesDir, filename)
            
            FileOutputStream(file).use { fos ->
                val writer = PrintWriter(fos)
                writer.println("Crash time: $timestamp")
                writer.println("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                writer.println("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}")
                writer.println("\nStack trace:")
                throwable.printStackTrace(writer)
                writer.flush()
            }
        } catch (e: Exception) {
            Log.e("CrashHandler", "Failed to write crash log", e)
        }
    }
}
```

## Diagnostic Tools

### Manual Database Inspection

If the app crashes due to database issues, you can extract and inspect the database using ADB:

```bash
# Pull database file from device
adb shell "run-as com.example.gotouchthatgrass_3 cat /data/data/com.example.gotouchthatgrass_3/databases/gotouchthatgrass_database" > db_dump.sqlite

# Open with SQLite
sqlite3 db_dump.sqlite

# Check schema
.tables
.schema blocked_apps
.schema challenges
```

### Enabling Strict Mode for Development

Add StrictMode to catch potential disk/network operations on the main thread:

```kotlin
// In MainApplication.onCreate()
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .penaltyLog()
            .build()
    )
    
    StrictMode.setVmPolicy(
        StrictMode.VmPolicy.Builder()
            .detectLeakedSqlLiteObjects()
            .detectLeakedClosableObjects()
            .penaltyLog()
            .build()
    )
}
```

## Crash Prevention Checklist

Before deploying a new version, use this checklist to ensure crash-free operation:

1. **Database Migrations**: Verify all schema changes have corresponding migrations
2. **Resource References**: Ensure all referenced resources (drawables, strings) exist
3. **Permissions**: Test on devices with permissions denied/granted
4. **Device Compatibility**: Test on older Android versions (especially API 23-26)
5. **Service Lifecycle**: Verify proper foreground service startup and shutdown
6. **Memory Usage**: Check for memory leaks and excessive bitmap allocations
7. **Threading**: Ensure long operations run on background threads
8. **Null Safety**: Add null checks for all external inputs and database results
9. **Exception Handling**: Wrap critical operations in try-catch blocks

## Recovery Mechanism

Implement a mechanism to detect and recover from previous crashes:

```kotlin
// In MainActivity.onCreate()
private fun checkForPreviousCrash() {
    val preferences = getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
    if (preferences.getBoolean("has_crashed", false)) {
        // Clear crash flag
        preferences.edit().putBoolean("has_crashed", false).apply()
        
        // Show recovery dialog
        AlertDialog.Builder(this)
            .setTitle("App Recovery")
            .setMessage("The app previously crashed. Would you like to:") 
            .setPositiveButton("Reset App Data") { _, _ ->
                resetAppData()
            }
            .setNegativeButton("Continue Normally") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}

private fun resetAppData() {
    // Reset preferences
    PreferenceManager(this).resetAllPreferences()
    
    // Clear database
    lifecycleScope.launch {
        withContext(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(this@MainActivity)
                database.clearAllTables()
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to clear database", e)
            }
        }
        
        Toast.makeText(this@MainActivity, "App data has been reset", Toast.LENGTH_SHORT).show()
        
        // Restart the app
        val intent = Intent(this@MainActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
    }
}
```
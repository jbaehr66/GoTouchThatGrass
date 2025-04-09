# Go Touch That Grass - Technical Deep Dive

This document provides a comprehensive technical reference for the Go Touch That Grass application, including edge cases, internal implementations, performance considerations, and debugging guidance.

## Grass Detection System

### Detection Algorithm

The app employs a dual-method approach to grass detection for balance between accuracy and reliability:

```kotlin
suspend fun isGrassInImage(imageBitmap: Bitmap): Boolean {
    // Check for green color dominance with a lower threshold for better sensitivity
    val hasGreenDominance = hasGreenColor(imageBitmap, threshold = 0.25f)
    
    // Try ML detection with fallback to color detection
    val mlDetection = try {
        detectGrassUsingML(imageBitmap)
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    // If either method detects grass with high confidence, consider it valid
    return hasGreenDominance || mlDetection
}
```

#### Color Detection Implementation

The color detection algorithm samples pixels at regular intervals to determine if an image contains sufficient green content:

```kotlin
fun hasGreenColor(bitmap: Bitmap, threshold: Float = 0.3f): Boolean {
    var greenPixelCount = 0
    val totalPixels = bitmap.width * bitmap.height
    
    // Sample pixels at intervals of 10 for performance
    for (x in 0 until bitmap.width step 10) {
        for (y in 0 until bitmap.height step 10) {
            val pixel = bitmap.getPixel(x, y)
            val red = (pixel shr 16) and 0xff
            val green = (pixel shr 8) and 0xff
            val blue = pixel and 0xff
            
            // Green channel must be significantly higher than red or blue
            if (green > red * 1.2 && green > blue * 1.2) {
                greenPixelCount++
            }
        }
    }
    
    val sampledPixels = (bitmap.width / 10) * (bitmap.height / 10)
    return greenPixelCount.toFloat() / sampledPixels > threshold
}
```

**Edge Cases:**
- **Evening/Night Photos**: Low light conditions may cause failure as greens appear darker
- **Brown/Dry Grass**: May fail color detection but can still be recognized by ML
- **Reflected Light**: Strong sunlight reflecting off grass can skew color readings
- **Green Indoor Objects**: Can produce false positives with color detection

**Performance Note:** The step size of 10 provides a balance between accuracy and performance. On lower-end devices, consider increasing to 15 or 20 if detection is slow.

#### ML Detection Implementation

The ML detection uses Google's ML Kit image labeling to identify grass-related objects:

```kotlin
suspend fun detectGrassUsingML(imageBitmap: Bitmap): Boolean = withContext(Dispatchers.Default) {
    suspendCancellableCoroutine { continuation ->
        try {
            val inputImage = InputImage.fromBitmap(imageBitmap, 0)
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
            
            labeler.process(inputImage)
                .addOnSuccessListener { labels ->
                    val detectedGrass = labels.any { label ->
                        grassLabels.any { grassType ->
                            label.text.lowercase().contains(grassType) && label.confidence > 0.6f
                        }
                    }
                    continuation.resume(detectedGrass)
                }
                .addOnFailureListener {
                    // Fallback to basic color detection if ML fails
                    continuation.resume(hasGreenColor(imageBitmap))
                }
        } catch (e: IOException) {
            continuation.resume(hasGreenColor(imageBitmap))
        }
    }
}
```

**Debugging Tips:**
- For ML detection issues, check logcat for ML Kit specific errors
- Common failure: insufficient RAM causing ML Kit to crash silently
- Test with various grass images in different lighting conditions
- If consistent failures in specific environments, adjust confidence threshold

## App Blocking System

### Current Foreground App Detection

```kotlin
fun getCurrentForegroundApp(): String? {
    if (!hasUsageStatsPermission()) return null
    
    try {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        // Get usage stats for the last 10 seconds
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, time - 10000, time
        )
        
        // Find the last used app
        if (stats != null) {
            var lastUsedApp: UsageStats? = null
            for (usageStats in stats) {
                if (lastUsedApp == null || usageStats.lastTimeUsed > lastUsedApp.lastTimeUsed) {
                    lastUsedApp = usageStats
                }
            }
            
            return lastUsedApp?.packageName
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error getting current foreground app", e)
    }
    
    return null
}
```

**Edge Cases:**
- **Split-screen mode**: Only detects the most recently interacted app
- **Overlay apps**: May not be detected as foreground
- **Quick app switching**: Rapid switching can cause detection delays
- **System dialogs**: Can interfere with detection accuracy

**Workaround for Android 11+:** On newer Android versions, the system limits usage stats access. In these cases, using the accessibility service can provide more reliable detection.

### Service Persistence

The service uses several techniques to maintain persistence:

```kotlin
// In AppBlockerService

// 1. Acquiring wake lock to prevent CPU sleep
private fun acquireWakeLock() {
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    wakeLock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "GoTouchThatGrass:AppBlockerWakeLock"
    ).apply {
        acquire(TimeUnit.HOURS.toMillis(8)) // 8 hour max
    }
}

// 2. Self-restart on destruction
override fun onDestroy() {
    // Cancel all coroutines
    monitoringJob?.cancel()
    serviceScope.cancel()
    
    // Release wake lock
    releaseWakeLock()
    
    super.onDestroy()
    
    // Restart service if it was killed
    val restartIntent = Intent(applicationContext, AppBlockerService::class.java)
    applicationContext.startService(restartIntent)
}

// 3. Boot-time restart via receiver
// In BootReceiver.kt
override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
        val serviceIntent = Intent(context, AppBlockerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
```

**Battery Impact Considerations:**

1. **Wake Lock Usage:** The partial wake lock prevents the CPU from sleeping while allowing the screen to turn off. This consumes approximately 3-5% additional battery per hour.

2. **App Monitoring Interval:** The 3-second polling interval (`MONITOR_INTERVAL = 3000L`) balances responsiveness with battery usage. Increasing this value to 5-10 seconds would significantly reduce battery impact at the cost of slower blocking response.

3. **ML Kit Operations:** Image processing is the most battery-intensive operation. Consider implementing a "lite mode" option in settings that would rely only on color detection.

## Streak Tracking System

### Streak Calculation Logic

```kotlin
private fun updateStreak() {
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_YEAR)
    
    val lastChallengeTime = preferenceManager.lastChallengeTimestamp
    val lastChallengeCalendar = Calendar.getInstance().apply {
        timeInMillis = lastChallengeTime
    }
    val lastChallengeDay = lastChallengeCalendar.get(Calendar.DAY_OF_YEAR)
    
    // Set current time as last challenge timestamp
    preferenceManager.lastChallengeTimestamp = System.currentTimeMillis()
    
    // Update streak
    if (lastChallengeTime == 0L ||
        (today - lastChallengeDay == 1 && calendar.get(Calendar.YEAR) == lastChallengeCalendar.get(Calendar.YEAR)) ||
        (lastChallengeCalendar.get(Calendar.YEAR) < calendar.get(Calendar.YEAR) &&
                lastChallengeCalendar.get(Calendar.DAY_OF_YEAR) == lastChallengeCalendar.getActualMaximum(Calendar.DAY_OF_YEAR) &&
                today == 1)
    ) {
        // Increment streak (either first challenge, consecutive day, or consecutive across year boundary)
        val newStreak = preferenceManager.streak + 1
        preferenceManager.streak = newStreak
        
        // Check for milestone (every 7 days)
        if (newStreak > 0 && newStreak % 7 == 0) {
            NotificationHelper(this).sendStreakMilestoneNotification(newStreak)
        }
    } else if (today != lastChallengeDay || calendar.get(Calendar.YEAR) != lastChallengeCalendar.get(Calendar.YEAR)) {
        // Reset streak if not consecutive
        preferenceManager.streak = 1
    }
}
```

**Edge Cases:**
- **Timezone Changes**: User traveling across time zones may get an unexpected streak reset or double-counting
- **System Clock Changes**: Manual clock adjustments can break streak calculation
- **Year Boundary**: Special case for maintaining streak from December 31 to January 1
- **Delayed Challenges**: Completing challenges late at night then early next morning registers as two consecutive days despite being <24 hours

**Internal Storage:**
Streak data is stored in SharedPreferences with these keys:
- `last_challenge_timestamp`: Unix timestamp of last successful challenge
- `current_streak`: Integer count of consecutive days with challenges

## Database Operations

### Challenge Insertion and Query Optimization

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

**Performance Notes:**
- Use of `LIMIT 1` and `COUNT(*)` optimizes query performance by minimizing data retrieval
- `Flow` return types allow reactive UI updates with minimal database polling
- Indexes are automatically created on primary keys, but consider adding indexes for frequently queried fields:

```kotlin
@Entity(
    tableName = "challenges",
    indices = [Index(value = ["timestamp"], unique = false)]
)
data class Challenge(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var timestamp: Long = System.currentTimeMillis(),
    var photoPath: String = "",
    var notes: String = "",
    var isSuccessful: Boolean = true
)
```

### Photo Storage Management

The app stores challenge photos in the external media directory:

```kotlin
private fun getOutputDirectory(): File {
    val mediaDir = externalMediaDirs.firstOrNull()?.let {
        File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
    }
    return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
}
```

**Potential Issues:**
- **Storage Growth**: Photos accumulate over time without cleanup
- **Privacy Risk**: Sensitive outdoor location photos stored unencrypted
- **File Permissions**: Android scoped storage changes in API 30+ may affect access

**Implementation Recommendation:**
Add a cleanup worker that runs weekly to remove photos older than 30 days:

```kotlin
class PhotoCleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val photoDir = getPhotoDirectory()
            val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
            
            photoDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    file.delete()
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
    
    private fun getPhotoDirectory(): File {
        val mediaDir = applicationContext.externalMediaDirs.firstOrNull()?.let {
            File(it, applicationContext.getString(R.string.app_name))
        }
        return mediaDir ?: applicationContext.filesDir
    }
}
```

## Debugging Guidance

### Common Issues and Solutions

#### 1. App Blocking Not Working

**Symptoms:**
- Blocked apps still open without challenge prompt
- Inconsistent blocking behavior

**Troubleshooting:**
1. **Check Usage Stats Permission:**
   ```kotlin
   adb shell dumpsys usagestats | grep <app_package_name>
   ```
   Expected output should show the package with access granted.

2. **Verify Service Running:**
   ```kotlin
   adb shell dumpsys activity services | grep AppBlockerService
   ```
   Should show service in running state.

3. **Force Package Name Match Check:**
   The most common issue is that the `packageName` inside `AppBlockerService` doesn't match actual package name:
   ```kotlin
   // Incorrect code in AppBlockerService.kt line 78:
   if (packageName == packageName) {
       delay(MONITOR_INTERVAL)
       return@let
   }
   
   // Should be:
   if (packageName == context.packageName) {
       delay(MONITOR_INTERVAL)
       return@let
   }
   ```

#### 2. Grass Detection Failing

**Symptoms:**
- Photos of grass consistently fail verification
- ML Kit errors in logcat

**Troubleshooting:**
1. **Check Device Compatibility:**
   ML Kit requires ARCore support. Verify with:
   ```kotlin
   adb shell dumpsys package com.google.ar.core
   ```

2. **Verify Image Processing:**
   Add debug code to `GrassDetector.kt`:
   ```kotlin
   fun debugImageValues(bitmap: Bitmap): String {
       var redAvg = 0
       var greenAvg = 0
       var blueAvg = 0
       var count = 0
       
       for (x in 0 until bitmap.width step 20) {
           for (y in 0 until bitmap.height step 20) {
               val pixel = bitmap.getPixel(x, y)
               redAvg += (pixel shr 16) and 0xff
               greenAvg += (pixel shr 8) and 0xff
               blueAvg += pixel and 0xff
               count++
           }
       }
       
       redAvg /= count
       greenAvg /= count
       blueAvg /= count
       
       return "R:$redAvg G:$greenAvg B:$blueAvg"
   }
   ```

3. **Add ML Kit Label Logging:**
   ```kotlin
   labeler.process(inputImage)
       .addOnSuccessListener { labels ->
           // Log all detected labels with confidence
           labels.forEach { label ->
               Log.d("GrassDetector", "Label: ${label.text}, Confidence: ${label.confidence}")
           }
           
           val detectedGrass = labels.any { /* existing code */ }
           continuation.resume(detectedGrass)
       }
   ```

#### 3. Service Termination

**Symptoms:**
- App blocking stops after device idle
- Service not restarting after termination

**Troubleshooting:**
1. **Check Battery Optimization:**
   Some devices aggressively kill background services. Users should:
   - Go to Settings → Battery → Battery Optimization
   - Find "Go Touch That Grass" and select "Don't optimize"

2. **Add Foreground Service Type:**
   For Android 10+, specify foreground service type:
   ```kotlin
   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
       serviceIntent.putExtra(ForegroundService.EXTRA_FOREGROUND_SERVICE_TYPE, 
                             ForegroundService.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
   }
   ```

3. **Implement Service Binding:**
   Add binding support to increase service priority:
   ```kotlin
   private val binder = LocalBinder()
   
   inner class LocalBinder : Binder() {
       fun getService(): AppBlockerService = this@AppBlockerService
   }
   
   override fun onBind(intent: Intent): IBinder {
       return binder
   }
   ```

## Performance Optimizations

1. **Reduce Detection Service Polling:**
   Current 3-second polling interval is aggressive. Consider adaptive polling based on app usage patterns:
   ```kotlin
   private var pollingInterval = 3000L
   
   private fun adjustPollingInterval() {
       val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
       
       // Slower polling at night when usage is likely lower
       pollingInterval = when {
           hour in 0..5 -> 10000L  // 10 seconds from midnight to 6am
           hour in 6..8 -> 5000L   // 5 seconds during morning hours
           hour in 22..23 -> 5000L // 5 seconds in late evening
           else -> 3000L           // 3 seconds during active hours
       }
   }
   ```

2. **Optimize Image Processing:**
   Resize images before processing to reduce memory usage:
   ```kotlin
   private fun resizeForProcessing(original: Bitmap, maxSize: Int = 800): Bitmap {
       val width = original.width
       val height = original.height
       
       val ratio = maxSize.toFloat() / Math.max(width, height)
       val newWidth = (width * ratio).toInt()
       val newHeight = (height * ratio).toInt()
       
       return Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
   }
   ```

3. **Lazy ML Kit Initialization:**
   Initialize ML Kit on demand rather than at startup:
   ```kotlin
   private var labeler: ImageLabeler? = null
   
   private fun getLabeler(): ImageLabeler {
       if (labeler == null) {
           labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
       }
       return labeler!!
   }
   ```

## Internal JSON Schemas

### Challenge Entity Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Challenge",
  "type": "object",
  "properties": {
    "id": {
      "type": "integer",
      "description": "Unique identifier for the challenge"
    },
    "timestamp": {
      "type": "integer",
      "description": "Unix timestamp when the challenge was completed"
    },
    "photoPath": {
      "type": "string",
      "description": "File path to the saved challenge photo"
    },
    "notes": {
      "type": "string",
      "description": "Optional user notes about the challenge"
    },
    "isSuccessful": {
      "type": "boolean",
      "description": "Whether the challenge was successfully completed"
    }
  },
  "required": ["id", "timestamp", "isSuccessful"]
}
```

### BlockedApp Entity Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "BlockedApp",
  "type": "object",
  "properties": {
    "packageName": {
      "type": "string",
      "description": "Android package name, unique identifier for the app"
    },
    "appName": {
      "type": "string",
      "description": "Human-readable app name for display"
    },
    "isCurrentlyBlocked": {
      "type": "boolean",
      "description": "Whether the app is currently blocked"
    },
    "blockStartTime": {
      "type": "integer",
      "description": "Unix timestamp when the app was first blocked"
    }
  },
  "required": ["packageName", "appName", "isCurrentlyBlocked"]
}
```
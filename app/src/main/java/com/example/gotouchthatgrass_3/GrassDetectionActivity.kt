package com.example.gotouchthatgrass_3

import android.content.Intent
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import kotlinx.coroutines.async
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.gotouchthatgrass_3.databinding.ActivityGrassDetectionBinding
import com.example.gotouchthatgrass_3.db.AppDatabase
import com.example.gotouchthatgrass_3.models.Challenge
import com.example.gotouchthatgrass_3.ui.theme.FontLoader
import com.example.gotouchthatgrass_3.util.AppBlockManager
import com.example.gotouchthatgrass_3.util.GrassDetector
import com.example.gotouchthatgrass_3.util.NotificationHelper
import com.example.gotouchthatgrass_3.util.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GrassDetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGrassDetectionBinding
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var grassDetector: GrassDetector
    private lateinit var appBlockManager: AppBlockManager
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var appDatabase: AppDatabase

    private var photoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGrassDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        grassDetector = GrassDetector(this)
        appBlockManager = AppBlockManager(this)
        preferenceManager = PreferenceManager(this)
        appDatabase = AppDatabase.getDatabase(this)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Load fonts asynchronously using FontLoader
        loadFonts()
        
        startCamera()

        binding.captureButton.setOnClickListener {
            takePhoto()
        }

        binding.confirmButton.setOnClickListener {
            verifyGrassAndUnblock()
        }

        binding.retryButton.setOnClickListener {
            binding.previewImage.visibility = View.GONE
            binding.confirmButton.visibility = View.GONE
            binding.retryButton.visibility = View.GONE
            binding.skipForNowText.visibility = View.GONE
            binding.viewFinder.visibility = View.VISIBLE
            binding.captureButton.visibility = View.VISIBLE
        }
        
        // Initialize the Skip for Now button
        binding.skipForNowText.setOnClickListener {
            Log.d("GrassDetection", "User skipped grass check")
            navigateToHome()
        }
    }
    
    private fun loadFonts() {
        try {
            // Load fonts for TextViews asynchronously
            FontLoader.loadFontAsync(lifecycleScope, this, binding.instructionText, R.font.poppins_medium)
            FontLoader.loadFontAsync(lifecycleScope, this, binding.captureButton, R.font.poppins_medium)
            FontLoader.loadFontAsync(lifecycleScope, this, binding.confirmButton, R.font.poppins_medium)
            FontLoader.loadFontAsync(lifecycleScope, this, binding.retryButton, R.font.poppins_medium)
            FontLoader.loadFontAsync(lifecycleScope, this, binding.skipForNowText, R.font.poppins_light)
        } catch (e: Exception) {
            Log.e("GrassDetectionActivity", "Error loading fonts", e)
            // Continue without custom fonts if there's an error
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Camera setup failed", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        
        // Show loading indicator or disable button to prevent multiple clicks
        binding.captureButton.isEnabled = false

        // Move file operations to background thread
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Create temporary file to store the image on background thread
                val outputDir = getOutputDirectory()
                val photoFile = File(
                    outputDir,
                    SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US)
                        .format(System.currentTimeMillis()) + ".jpg"
                )
                
                // Switch back to main thread for camera operations
                withContext(Dispatchers.Main) {
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    
                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(this@GrassDetectionActivity),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                this@GrassDetectionActivity.photoFile = photoFile
                                showPreview(photoFile)
                                binding.captureButton.isEnabled = true
                            }
    
                            override fun onError(exception: ImageCaptureException) {
                                binding.captureButton.isEnabled = true
                                Toast.makeText(
                                    this@GrassDetectionActivity,
                                    "Failed to capture image: ${exception.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("GrassDetection", "Error preparing photo file", e)
                withContext(Dispatchers.Main) {
                    binding.captureButton.isEnabled = true
                    Toast.makeText(
                        this@GrassDetectionActivity,
                        "Failed to prepare camera: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showPreview(photoFile: File) {
        // Decode bitmap on background thread to avoid StrictMode violations
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply {
                    // Sample down if the image is large
                    inSampleSize = 1
                }
                
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath, options)
                
                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    binding.previewImage.setImageBitmap(bitmap)
                    
                    // Update UI
                    binding.viewFinder.visibility = View.GONE
                    binding.captureButton.visibility = View.GONE
                    binding.previewImage.visibility = View.VISIBLE
                    binding.confirmButton.visibility = View.VISIBLE
                    binding.retryButton.visibility = View.VISIBLE
                    binding.skipForNowText.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("GrassDetection", "Error loading preview image", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@GrassDetectionActivity,
                        "Error loading image preview: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun verifyGrassAndUnblock() {
        binding.progressBar.visibility = View.VISIBLE
        binding.confirmButton.isEnabled = false
        binding.retryButton.isEnabled = false
        binding.instructionText.text = "Analyzing your photo..."

        lifecycleScope.launch {
            val photoFile = this@GrassDetectionActivity.photoFile
            
            // Check if file exists on IO dispatcher to avoid StrictMode violation
            val fileExists = if (photoFile != null) {
                withContext(Dispatchers.IO) {
                    photoFile.exists()
                }
            } else {
                false
            }
            
            if (photoFile == null || !fileExists) {
                Toast.makeText(this@GrassDetectionActivity, "Error: Photo file not found", Toast.LENGTH_SHORT).show()
                binding.instructionText.text = "Please try taking a photo again"
                binding.progressBar.visibility = View.GONE
                binding.confirmButton.isEnabled = true
                binding.retryButton.isEnabled = true
                return@launch
            }

            // Get bitmap for analysis with sampling to prevent OutOfMemoryError
            val bitmap = withContext(Dispatchers.IO) {
                decodeSampledBitmap(photoFile, 1024, 1024)
            }

            try {
                // Check if the image contains grass using both color detection and ML
                val containsGrass = grassDetector.isGrassInImage(bitmap)

                if (containsGrass) {
                    try {
                        Log.d("GrassDetection", "Grass detected in image, proceeding with challenge")
                        
                        try {
                            // Save successful challenge to database
                            saveChallenge(photoFile.absolutePath, true)
                            
                            // Unblock all apps - ensure on background thread
                            withContext(Dispatchers.IO) {
                                appBlockManager.unblockAllApps()
                            }
    
                            // Update last challenge timestamp and streak
                            updateStreak()
    
                            // Show success animation or feedback
                            withContext(Dispatchers.Main) {
                                binding.progressBar.visibility = View.GONE
                                binding.instructionText.text = "Success! You've touched grass!"
                                binding.instructionText.setBackgroundColor(resources.getColor(R.color.success_green, null))
                            }
    
                            // Delay to show the success message
                            delay(1500)
    
                            Toast.makeText(
                                this@GrassDetectionActivity,
                                "Great job! Your apps have been unblocked.",
                                Toast.LENGTH_LONG
                            ).show()
    
                            // Return to main activity
                            startActivity(Intent(this@GrassDetectionActivity, MainActivity::class.java))
                            finish()
                        } catch (dbError: Exception) {
                            Log.e("GrassDetection", "Database error while processing successful detection", dbError)
                            
                            // Even if saving fails, we should still unblock apps
                            withContext(Dispatchers.IO) {
                                appBlockManager.unblockAllApps()
                            }
                            
                            // Show a warning but don't block the success path completely
                            Toast.makeText(
                                this@GrassDetectionActivity,
                                "Warning: Couldn't save your achievement, but apps have been unblocked.",
                                Toast.LENGTH_LONG
                            ).show()
                            
                            // Return to main activity
                            startActivity(Intent(this@GrassDetectionActivity, MainActivity::class.java))
                            finish()
                        }
                    } catch (e: Exception) {
                        Log.e("GrassDetection", "Error processing successful detection", e)
                        showError("Error processing challenge. Please try again.")
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.confirmButton.isEnabled = true
                    binding.retryButton.isEnabled = true
                    binding.instructionText.text = "We couldn't detect grass. Try again!"
                    binding.instructionText.setBackgroundColor(resources.getColor(R.color.failure_red, null))

                    Toast.makeText(
                        this@GrassDetectionActivity,
                        "We couldn't detect grass in your photo. Please try again!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("GrassDetection", "Error analyzing photo", e)
                showError("Error analyzing photo. Please try again.")
            }
        }
    }
    
    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.confirmButton.isEnabled = true
        binding.retryButton.isEnabled = true
        binding.instructionText.text = message
        binding.instructionText.setBackgroundColor(resources.getColor(R.color.failure_red, null))
        
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    /**
     * Decodes a bitmap with sampling to reduce memory usage
     * 
     * Note: This function must ONLY be called from a background thread (Dispatchers.IO)
     * It performs multiple file I/O operations that would trigger StrictMode violations on the main thread
     */
    private fun decodeSampledBitmap(photoFile: File, reqWidth: Int, reqHeight: Int): Bitmap {
        // Make sure we're not on the main thread
        assert(Thread.currentThread().name != "main") { "decodeSampledBitmap must be called from a background thread" }
        
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

    private suspend fun saveChallenge(photoPath: String, successful: Boolean) {
        try {
            Log.d("GrassDetection", "Attempting to save challenge with photo: $photoPath")
            
            val challenge = Challenge(
                timestamp = System.currentTimeMillis(),
                photoPath = photoPath.ifEmpty { "" },  // Ensure photoPath is never null
                isSuccessful = successful,
                notes = ""
            )

            val result = withContext(Dispatchers.IO) {
                try {
                    val insertId = appDatabase.challengeDao().insert(challenge)
                    Log.d("GrassDetection", "Challenge saved successfully with ID: $insertId")
                    insertId
                } catch (dbError: Exception) {
                    Log.e("GrassDetection", "Database error while saving challenge", dbError)
                    throw dbError // Re-throw to be caught by outer try-catch
                }
            }
            
            if (result <= 0) {
                Log.w("GrassDetection", "Challenge saved but returned invalid ID: $result")
            }
        } catch (e: Exception) {
            Log.e("GrassDetection", "Failed to save challenge", e)
            // More detailed logging
            when (e) {
                is SQLiteException -> Log.e("GrassDetection", "SQLite error: ${e.message}")
                is IllegalStateException -> Log.e("GrassDetection", "Database not open: ${e.message}")
                else -> Log.e("GrassDetection", "Unknown error: ${e.message}")
            }
            throw e // Re-throw so the caller knows there was an error
        }
    }

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

    private fun getOutputDirectory(): File {
        // This function is already called from a background thread in the updated takePhoto method
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            val dir = File(it, resources.getString(R.string.app_name))
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }
    
    // A cache for the output directory to avoid disk operations
    private val outputDirectory by lazy {
        lifecycleScope.async(Dispatchers.IO) {
            getOutputDirectory()
        }
    }

    /**
     * Navigate back to the home screen
     * Sets appropriate flags to clear the activity stack
     */
    private fun navigateToHome() {
        // Show a snackbar message
        val rootView = binding.root
        com.google.android.material.snackbar.Snackbar.make(
            rootView,
            getString(R.string.try_again_later),
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
        
        // Short delay to allow the Snackbar to be seen
        lifecycleScope.launch {
            delay(500)
            
            // Navigate to MainActivity
            val intent = Intent(this@GrassDetectionActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
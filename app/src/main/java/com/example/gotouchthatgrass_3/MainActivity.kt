package com.example.gotouchthatgrass_3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.gotouchthatgrass_3.databinding.ActivityMainBinding
import com.example.gotouchthatgrass_3.db.AppDatabase
import com.example.gotouchthatgrass_3.service.AppBlockerService
import com.example.gotouchthatgrass_3.ui.theme.FontLoader
import com.example.gotouchthatgrass_3.util.AppBlockManager
import com.example.gotouchthatgrass_3.util.DailyReminderWorker
import com.example.gotouchthatgrass_3.util.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBlockManager: AppBlockManager
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var appDatabase: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check for previous crashes
        checkForPreviousCrash()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up the toolbar as ActionBar
        setSupportActionBar(binding.toolbar)

        // Initialize database with crash protection
        try {
            appDatabase = AppDatabase.getDatabase(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "Database initialization failed", e)
            Toast.makeText(this, "Database initialization failed, resetting database", Toast.LENGTH_LONG).show()
            // The getDatabase method now handles resetting the database on failure
        }

        appBlockManager = AppBlockManager(this)
        preferenceManager = PreferenceManager(this)
        
        // Load fonts for navigation items and text in the BottomNavigationView
        loadFonts()

        val navView: BottomNavigationView = binding.navView
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_stats, R.id.navigation_blocked_apps
            )
        )
        // Explicitly specify the overload to resolve ambiguity
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        requestRequiredPermissions()
        setupNotifications()
        
        // Start the app blocker service with error handling
        try {
            val serviceIntent = Intent(this, AppBlockerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start app blocking service", e)
            Toast.makeText(this, "Failed to start app blocking service", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestRequiredPermissions() {
        val missingPermissions = mutableListOf<String>()
        
        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.CAMERA)
        }
        
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Request permissions if needed
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                CAMERA_PERMISSION_REQUEST
            )
        }

        // Check usage stats permission (requires special handling)
        if (!appBlockManager.hasUsageStatsPermission()) {
            showUsageStatsPermissionDialog()
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

    private fun setupNotifications() {
        if (preferenceManager.notificationsEnabled) {
            val reminderRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
                24, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "daily_reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                reminderRequest
            )
        }
    }
    
    private fun checkForPreviousCrash() {
        val preferences = getSharedPreferences("crash_prefs", MODE_PRIVATE)
        if (preferences.getBoolean("has_crashed", false)) {
            // Clear crash flag
            preferences.edit().putBoolean("has_crashed", false).apply()
            
            // Show recovery dialog
            AlertDialog.Builder(this)
                .setTitle("App Recovery")
                .setMessage("The app previously crashed. Would you like to reset app data or continue normally?") 
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
        preferenceManager.streak = 0
        preferenceManager.lastChallengeTimestamp = 0
        
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted
            } else {
                Toast.makeText(this, "Camera permission is required to take grass photos", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadFonts() {
        // We can't directly apply fonts to Toolbar or BottomNavigationView
        // Menu items will use the default font styles from the theme
        
        // Note: If we need to set fonts for toolbar title, we'd need to do:
        // val titleTextView = findToolbarTitleTextView(binding.toolbar)
        // if (titleTextView != null) {
        //     FontLoader.loadFontAsync(lifecycleScope, this, titleTextView, R.font.poppins_medium)
        // }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }
}

//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.tooling.preview.Preview
//import com.example.gotouchthatgrass_3.ui.theme.GoTouchThatGrass3Theme
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContent {
//            GoTouchThatGrass3Theme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    GoTouchThatGrass3Theme {
//        Greeting("Android")
//    }
//}
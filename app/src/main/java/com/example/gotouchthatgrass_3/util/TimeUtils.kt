package com.example.gotouchthatgrass_3.util

import java.util.concurrent.TimeUnit

/**
 * Utility class for time and date related operations.
 */
object TimeUtils {
    
    /**
     * Format a duration in milliseconds to a human-readable string.
     * 
     * @param timeMillis The time in milliseconds to format
     * @return A human-readable string representing the time duration
     */
    fun formatRelativeTime(timeMillis: Long): String {
        val now = System.currentTimeMillis()
        val durationMillis = now - timeMillis
        
        return when {
            durationMillis < TimeUnit.MINUTES.toMillis(1) -> "just now"
            durationMillis < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
                "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
            }
            durationMillis < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
                "$hours ${if (hours == 1L) "hour" else "hours"} ago"
            }
            durationMillis < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(durationMillis)
                "$days ${if (days == 1L) "day" else "days"} ago"
            }
            else -> {
                val days = TimeUnit.MILLISECONDS.toDays(durationMillis)
                "$days days ago"
            }
        }
    }
    
    /**
     * Format a millisecond timestamp to a human-readable time string (HH:mm format).
     * 
     * @param timeMillis The timestamp in milliseconds
     * @return A string in the format "HH:mm"
     */
    fun formatTime(timeMillis: Long): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timeMillis
        
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        
        return String.format("%02d:%02d", hour, minute)
    }
    
    /**
     * Format a millisecond timestamp to a human-readable date (MM/dd/yyyy format).
     * 
     * @param timeMillis The timestamp in milliseconds
     * @return A string in the format "MM/dd/yyyy"
     */
    fun formatDate(timeMillis: Long): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timeMillis
        
        val month = calendar.get(java.util.Calendar.MONTH) + 1 // Month is 0-based
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val year = calendar.get(java.util.Calendar.YEAR)
        
        return String.format("%02d/%02d/%04d", month, day, year)
    }
}
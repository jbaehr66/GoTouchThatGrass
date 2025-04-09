package com.example.gotouchthatgrass_3.models

/**
 * Enum representing categories for apps to help organize them in the UI.
 */
enum class AppCategory(val displayName: String) {
    SOCIAL("Social"),
    GAMES("Games"),
    PRODUCTIVITY("Productivity"),
    ENTERTAINMENT("Entertainment"),
    COMMUNICATION("Communication"),
    UTILITIES("Utilities"),
    OTHER("Other");
    
    companion object {
        /**
         * Determines the most likely category for an app based on its package name and/or app name.
         */
        fun categorizeApp(packageName: String, appName: String): AppCategory {
            val pkgLower = packageName.lowercase()
            val nameLower = appName.lowercase()
            
            return when {
                // Social media apps
                pkgLower.contains("facebook") || pkgLower.contains("instagram") || 
                pkgLower.contains("twitter") || pkgLower.contains("tiktok") ||
                pkgLower.contains("snapchat") || pkgLower.contains("pinterest") ||
                pkgLower.contains("linkedin") || pkgLower.contains("reddit") ||
                nameLower.contains("social") -> SOCIAL
                
                // Games
                pkgLower.contains("game") || pkgLower.contains("play") ||
                nameLower.contains("game") || pkgLower.startsWith("com.supercell") ||
                pkgLower.startsWith("com.king") || pkgLower.contains("casino") ||
                nameLower.contains("puzzle") || nameLower.contains("candy") -> GAMES
                
                // Productivity
                pkgLower.contains("docs") || pkgLower.contains("office") ||
                pkgLower.contains("sheet") || pkgLower.contains("slide") ||
                pkgLower.contains("word") || pkgLower.contains("excel") ||
                pkgLower.contains("powerpoint") || pkgLower.contains("note") ||
                pkgLower.contains("calendar") || pkgLower.contains("task") ||
                nameLower.contains("productivity") -> PRODUCTIVITY
                
                // Entertainment
                pkgLower.contains("netflix") || pkgLower.contains("spotify") ||
                pkgLower.contains("youtube") || pkgLower.contains("hulu") ||
                pkgLower.contains("disney") || pkgLower.contains("music") ||
                pkgLower.contains("video") || pkgLower.contains("tv") ||
                pkgLower.contains("player") || pkgLower.contains("amazon") ||
                nameLower.contains("entertainment") -> ENTERTAINMENT
                
                // Communication
                pkgLower.contains("mail") || pkgLower.contains("message") ||
                pkgLower.contains("chat") || pkgLower.contains("talk") ||
                pkgLower.contains("whatsapp") || pkgLower.contains("telegram") ||
                pkgLower.contains("signal") || pkgLower.contains("discord") ||
                pkgLower.contains("skype") || pkgLower.contains("slack") ||
                nameLower.contains("communication") -> COMMUNICATION
                
                // Utilities
                pkgLower.contains("util") || pkgLower.contains("tool") ||
                pkgLower.contains("calc") || pkgLower.contains("clock") ||
                pkgLower.contains("weather") || pkgLower.contains("scan") ||
                pkgLower.contains("file") || pkgLower.contains("manager") ||
                nameLower.contains("utility") || nameLower.contains("utilities") -> UTILITIES
                
                // Default to Other
                else -> OTHER
            }
        }
    }
}
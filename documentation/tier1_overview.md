# Go Touch That Grass - System Overview

## Application Purpose

Go Touch That Grass is a digital wellbeing application designed to encourage users to engage with the natural world by temporarily blocking selected apps until the user completes a "grass-touching" challenge. The app employs a gamified approach to screen time management, rewarding users for spending time outdoors rather than simply restricting digital access.

## Core Architecture

The application follows a modern Android MVVM (Model-View-ViewModel) architecture with Room database, foreground services, and image detection capabilities:

```
                  ┌─────────────────┐
                  │    Activities   │
                  │  - Main         │
                  │  - GrassDetect  │
                  │  - Settings     │
                  └────────┬────────┘
                           │
                           ▼
┌───────────────┐   ┌─────────────────┐   ┌────────────────┐
│   Fragments   │◄──┤   ViewModels    │◄──┤  Repositories  │
│  - Home       │   │  - Home         │   │ (Implicit via  │
│  - BlockedApps│   │  - BlockedApps  │   │   Room DAOs)   │
│  - Stats      │   │  - Stats        │   │                │
└───────────────┘   └─────────────────┘   └───────┬────────┘
                                                   │
                                                   ▼
┌───────────────┐   ┌─────────────────┐   ┌────────────────┐
│  Foreground   │   │    Utilities    │   │   Database     │
│    Service    │◄──┤ - AppBlocker    │◄──┤  - BlockedApp  │
│ (App Monitor) │   │ - GrassDetector │   │  - Challenge   │
└───────────────┘   └─────────────────┘   └────────────────┘
```

## Key Components

### 1. App Blocking System
- **AppBlockerService**: Continuously runs as a foreground service to monitor app usage
- **AppBlockManager**: Handles app blocking logic and usage stat permissions
- **BootReceiver**: Ensures the blocking service starts on device boot

### 2. Grass Detection System
- **GrassDetectionActivity**: Camera interface for taking grass photos
- **GrassDetector**: Uses two verification methods:
  - Color-based detection (green pixel analysis)
  - ML Kit image labeling for identifying grass-related objects

### 3. Challenge & Tracking System
- **Challenge Entity**: Records successful outdoor activities
- **PreferenceManager**: Manages streak data and app settings
- **HomeFragment**: Displays challenge status and current streak

### 4. App Selection System
- **BlockedAppsFragment**: Interface for selecting apps to block
- **AppListAdapter**: Displays installed apps for selection

## Architecture Implications

1. **Privacy-Focused**: All processing happens on-device with no cloud dependencies
2. **Battery Considerations**: Foreground service requires optimization for battery life
3. **Permission Requirements**: Needs camera, usage stats, and notification permissions
4. **Resilience**: Service auto-restarts on termination to maintain blocking functionality
5. **Performance**: ML Kit integration requires appropriate hardware capabilities

## Integration Points

- **Android Usage Stats API**: For detecting foreground applications
- **Camera API**: For capturing grass photos
- **ML Kit**: For image analysis
- **Room Database**: For persistent storage
- **WorkManager**: For scheduling daily reminders
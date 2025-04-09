# Gradle Build Guide for Go Touch That Grass

This document provides comprehensive information about the Gradle build system used in the Go Touch That Grass Android project and how to resolve common setup issues in IntelliJ IDEA.

## Project Structure Overview

The Go Touch That Grass project is structured as a standard Android Gradle project:

- **Root Project**: GoTouchThatGrass-3
- **Module**: app (main application module)

The project uses a combination of both traditional Groovy build.gradle files and modern Kotlin DSL build.gradle.kts files.

## Key Gradle Files

| File | Purpose |
|------|---------|
| `settings.gradle.kts` | Configures project structure and repositories |
| `build.gradle.kts` | Root project build file with shared configuration |
| `app/build.gradle` | Application module-specific build configuration |
| `gradle/libs.versions.toml` | Version catalog for dependency management |

## Important Gradle Tasks

When set up correctly, the following tasks should be available:

### App Tasks
- `app:assembleDebug` - Assembles the debug APK
- `app:installDebug` - Installs the debug APK on a connected device
- `app:connectedDebugAndroidTest` - Runs Android tests on a connected device

### Build Tasks
- `assemble` - Assembles all variants of the application
- `build` - Builds the entire project
- `clean` - Deletes the build directory
- `test` - Runs all unit tests

### Android Tasks
- `uninstallAll` - Uninstalls all applications
- `lintDebug` - Runs lint on the Debug build
- `lintRelease` - Runs lint on the Release build
- `lintVitalRelease` - Runs lint checks that are marked as vital

## Dependency Management

Dependencies are managed in multiple ways:
1. Version catalog in `gradle/libs.versions.toml` (for versions and common dependencies)
2. Direct declarations in `app/build.gradle`

Major dependencies include:
- AndroidX Core and AppCompat
- Material Design Components
- Navigation Components
- Room Database
- CameraX
- ML Kit for image labeling
- WorkManager
- Kotlin Coroutines

## Resolving IntelliJ "Missing App Module" Issue

If you don't see the "app" module in the dropdown in IntelliJ IDEA, follow these troubleshooting steps:

### 1. Check JDK Configuration

One common issue is missing or incorrect JDK configuration:

1. Go to File > Project Structure > Project
2. Ensure a valid JDK is selected (JDK 17 is recommended for modern Android development)
3. If no JDK is available, click "New..." and point to your JDK installation or use the download option

### 2. Import the Project Correctly

Sometimes a project needs to be re-imported properly:

1. Close the project
2. Select "Import Project" instead of "Open Project"
3. Navigate to the `build.gradle` or `build.gradle.kts` file in the root directory
4. Select "Open as Project"
5. Use "Import using Gradle" option when prompted

### 3. Sync Gradle Files

Force a Gradle sync to rebuild the project structure:

1. Click on the "Gradle" tab on the right side of the IDE
2. Click the "Refresh" button (circular arrow)
3. Wait for the sync to complete

### 4. Check Module Settings

Verify module settings are correctly configured:

1. Go to File > Project Structure > Modules
2. If no modules are listed, click "+" and select "Import Module"
3. Navigate to the "app" directory and select the build.gradle file
4. Follow the import wizard

### 5. Manually Edit Configuration

If the module exists but doesn't appear in run configurations:

1. Go to Run > Edit Configurations
2. Click "+" and select "Android App"
3. In the "Module" dropdown, click "..." if "app" is not visible
4. Manually select the app module from the list of available modules

### 6. Check Gradle Wrapper

Ensure the Gradle wrapper is properly configured:

1. Open the `gradle/wrapper/gradle-wrapper.properties` file
2. Verify the distributionUrl points to a valid Gradle version
3. If needed, update to a compatible version like `gradle-8.0-bin.zip`

### 7. Install Required Plugins

Make sure you have the necessary plugins:

1. Go to File > Settings > Plugins
2. Verify that "Android Support" plugin is installed and enabled
3. Install it if missing

### 8. Check for Gradle JDK

Ensure Gradle is using the correct JDK:

1. Go to File > Settings > Build, Execution, Deployment > Build Tools > Gradle
2. Set "Gradle JVM" to the same JDK as your project

## Building without the IDE

If IDE issues persist, you can build from the command line:

```bash
# Navigate to project directory
cd "/Users/johnbridges/Dropbox/NewHope Psychology/git/thirdparty/GoTouchThatGrass-3-main"

# Clean and build debug APK
./gradlew clean assembleDebug

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test
```

## Manually Starting the App

If you can build but not run from IntelliJ:

1. Build the APK using `./gradlew assembleDebug`
2. Find the APK in `app/build/outputs/apk/debug/app-debug.apk`
3. Install manually via adb: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
4. Launch the app on the device via:
   ```
   adb shell am start -n com.example.gotouchthatgrass_3/.MainActivity
   ```

## Additional Resources

For more information about Android Gradle builds:
- [Android Gradle Plugin Documentation](https://developer.android.com/studio/build)
- [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)
- [Kotlin DSL Documentation](https://docs.gradle.org/current/userguide/kotlin_dsl.html)
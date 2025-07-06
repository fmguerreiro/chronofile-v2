# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Building
- `make apk` - Build release APK (requires Java 17+)
- `./gradlew assembleDebug` - Build debug APK
- `./gradlew assembleRelease` - Build release APK directly

### Development
- `make log` - Show filtered Android logs for debugging (requires connected device/emulator)
- `adb install app/build/outputs/apk/debug/app-debug.apk` - Install debug build
- `adb install app/build/outputs/apk/release/app-release.apk` - Install release build

### Testing
No automated tests exist in this codebase. Testing is done manually on device/emulator.

## Architecture Overview

### Redux-Inspired State Management
The app uses a unidirectional data flow pattern inspired by Redux:

- **Store.kt** - Central state container with actions and reducers
- **Actions** - Sealed class hierarchy defining all state mutations
- **State** - Immutable data class holding entire app state
- **Reducer** - Pure function `(State, Action) -> State`

### Core Components
- **MainActivity.kt** - Timeline view with FAB for adding entries
- **EditorActivity.kt** - JSON configuration editor
- **GraphActivity.kt** - Statistics with pie/area/radar charts
- **Entry.kt** - Time entry data model
- **History.kt** - Collection of entries with filtering/searching
- **Config.kt** - App configuration with JSON serialization

### Data Persistence
- **chronofile.tsv** - Timeline entries in tab-separated format
- **chronofile.json** - App configuration (activity groups, NFC tags)
- **Storage location** - Device-local files (100% offline)

### Key Libraries
- **MPAndroidChart** - Charts and graphs
- **RxJava/RxRelay** - Reactive state management
- **Gson** - JSON serialization for config
- **Material Components** - UI components

### Privacy Design
- No network permissions or internet access
- 100% offline operation
- Local file storage only
- No telemetry or analytics

## Development Notes

### Build Requirements
- Android Studio with Kotlin 2.0.21+
- Java 17+ (required for Gradle)
- Android SDK 35
- Minimum API 21, Target API 35

### Signing
Release builds require `keystore.properties` file with signing configuration.

### Code Style
- 100% Kotlin codebase
- Immutable data structures preferred
- Functional programming patterns
- Redux-style unidirectional data flow
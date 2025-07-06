# Chronofile

This is a personal time tracking app originally created by Art Chaidarun. More background at his [blog](https://chaidarun.com/ten-years-of-logging-my-life) and [Hacker News](https://news.ycombinator.com/item?id=29692087).

This fork includes significant modernizations and improvements:

## What's New in This Fork

### Material Design 3 Modernization
- **Timeline UI**: Completely redesigned with Material Design 3 components and improved visual hierarchy
- **Settings Page**: Modern interface with visual activity groups management
- **Statistics Page**: Updated charts and graphs with Material Design 3 styling
- **Consistent Design Language**: All screens now follow Material Design 3 guidelines

### Enhanced User Experience
- **Simplified Interface**: Removed note field and search button from main UI to reduce clutter
- **Better Search**: Improved search functionality with result count and duration display
- **Visual Activity Groups**: Settings page now provides visual management of activity categories
- **Permissions Management**: Added permission request banners with clear user guidance

### Technical Improvements
- **Modern Android Development**: Updated to latest Android SDK (API 35) and Gradle
- **Kotlin 2.0.21**: Updated to latest Kotlin version with modern language features
- **Storage Access Framework**: Replaced direct file I/O with SAF for better security and compatibility
- **View Binding**: Migrated from deprecated Kotlin synthetics to modern view binding
- **NFC Support**: Added NFC tag support for quick time entry
- **Dependency Updates**: All libraries updated to latest stable versions

### Privacy & Security
- **AGPL v3 License**: Switched to copyleft license ensuring derivative works remain open source
- **Zero Network Access**: Maintains 100% offline operation - no ads, no telemetry, nothing
- **Local Storage**: All data saved to device-local `chronofile.tsv` file

## Installation

You can try it out by installing an APK from the [releases](https://github.com/artnc/chronofile/releases) page of the original repository, or build from source using the instructions in [CONTRIBUTING.md](CONTRIBUTING.md).

<img alt="Timeline" src="https://raw.githubusercontent.com/artnc/chronofile/master/.github/Screenshot_20180103-223514.png" width="32%"> <img alt="Pie chart" src="https://raw.githubusercontent.com/artnc/chronofile/master/.github/Screenshot_20180103-222320.png" width="32%"> <img alt="Area chart" src="https://raw.githubusercontent.com/artnc/chronofile/master/.github/Screenshot_20180103-222328.png" width="32%">

## Build Requirements

- Android Studio with Kotlin 2.0.21+
- Java 17+ (required for Gradle)
- Android SDK 35
- Minimum API 21, Target API 35

## Architecture

The app uses a Redux-inspired unidirectional data flow pattern with:
- **Store.kt**: Central state container
- **Actions**: Sealed class hierarchy for state mutations
- **Immutable State**: Single source of truth
- **Pure Reducers**: Predictable state transformations

© 2017 Art Chaidarun (Original Author)

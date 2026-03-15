# Money Manager - Android Application

A comprehensive personal finance management application built with Kotlin using Clean Architecture. This app can be built entirely from the command line without Android Studio or Gradle.

## Features

### Core Features
- **Money Tracker**: Record income and expenses with categories
- **Budget Planning**: Set monthly budgets per category with progress tracking
- **Charts & Statistics**: Visualize spending with pie charts and monthly trends
- **CSV Import**: Import bank statements from CSV files
- **Auto Backup**: Automatic backup to JSON/CSV format
- **Export Reports**: Export transaction reports to CSV/Excel

### Dashboard
- Total balance overview
- Income/Expense summary
- Monthly statistics
- Recent transactions list

### Transaction Management
- Add/Edit/Delete transactions
- Filter by type, category, date range
- Categorize transactions automatically
- Duplicate detection during import

### Budget Planner
- Monthly budget per category
- Progress indicators with color coding
- Warning when approaching limits
- Remaining budget calculation

## Project Structure (Clean Architecture)

```
money-manager/
├── Makefile                    # Build system
├── AndroidManifest.xml         # App manifest
├── build.config                # Build configuration
├── src/
│   ├── MoneyManagerApplication.kt
│   ├── domain/
│   │   ├── model/              # Business entities
│   │   ├── repository/         # Repository interfaces
│   │   └── usecase/            # Business logic use cases
│   ├── data/
│   │   ├── database/           # SQLite database
│   │   ├── repository/         # Repository implementations
│   │   ├── datasource/         # Data sources
│   │   ├── importer/           # CSV importer
│   │   └── backup/             # Backup manager
│   └── presentation/
│       ├── activities/         # UI activities
│       ├── viewmodels/         # ViewModels
│       └── adapters/           # RecyclerView adapters
├── res/
│   ├── layout/                 # XML layouts
│   ├── values/                 # Strings, colors, themes
│   ├── drawable/               # Vector icons
│   └── menu/                   # Menu definitions
├── libs/                       # Downloaded dependencies
└── build/                      # Build output
```

## Prerequisites

### Required Tools
1. **Android SDK** (API 33 recommended)
   - Platform Tools
   - Build Tools 33.0.2
   - Android Platform API 33

2. **Kotlin Compiler** (kotlinc 1.9.20+)
   ```bash
   # Download from: https://github.com/JetBrains/kotlin/releases
   # Or use SDKMAN: sdk install kotlin
   ```

3. **Java JDK** (JDK 8 or 11)

4. **ADB** (Android Debug Bridge)

### Environment Setup

```bash
# Set Android SDK path
export ANDROID_SDK_ROOT=$HOME/Android/Sdk
export ANDROID_HOME=$ANDROID_SDK_ROOT

# Add tools to PATH
export PATH=$PATH:$ANDROID_SDK_ROOT/tools
export PATH=$PATH:$ANDROID_SDK_ROOT/platform-tools
export PATH=$PATH:$ANDROID_SDK_ROOT/build-tools/33.0.2
export PATH=$PATH:$KOTLIN_HOME/bin
```

## Build Instructions

### Download Dependencies

```bash
cd money-manager
make deps
```

This will download all required libraries to the `libs/` folder:
- Kotlin Standard Library
- Kotlin Coroutines
- MPAndroidChart
- AndroidX libraries
- Gson

### Build APK

```bash
# Build the APK
make build

# Or check dependencies first
make check-deps
make build
```

The APK will be generated at `money-manager.apk`

### Install to Device

```bash
# Connect your Android device via USB
# Enable USB Debugging in Developer Options

# Install APK
make install

# Or build, install, and run
make run
```

### View Logs

```bash
# View app logs
make log

# View filtered logs
make log-filter
```

### Other Commands

```bash
# Clean build files
make clean

# Clean everything including downloaded libs
make distclean

# Verify APK signature
make verify

# List connected devices
make devices

# Uninstall app
make uninstall

# Show help
make help
```

## Usage Guide

### Adding a Transaction

1. Open the app
2. Tap the floating action button (+)
3. Select transaction type (Income/Expense)
4. Enter amount
5. Select category
6. Add optional description
7. Set date
8. Tap Save

### Setting a Budget

1. Navigate to Budget tab
2. Tap the floating action button (+)
3. Select category
4. Enter monthly budget amount
5. Tap Save

### Importing CSV

1. Go to Settings
2. Tap "Import CSV (Bank Statement)"
3. Select CSV file from your device
4. Preview the transactions
5. Tap "Import Transactions"

**CSV Format:**
```csv
Date,Description,Amount
2025-01-02,Coffee Shop,-25000
2025-01-03,Salary Deposit,5000000
```

### Exporting Reports

1. Go to Settings
2. Tap "Export Report"
3. Select format (CSV/Excel)
4. Select time period
5. Tap "Export Report"

### Backup & Restore

**Manual Backup:**
1. Go to Settings
2. Tap "Backup Now"
3. Backup saved to Documents/MoneyManagerBackup/

**Auto Backup:**
- Enabled by default
- Creates backup when app closes
- Toggle in Settings

**Restore:**
1. Go to Settings
2. Tap "Restore"
3. Select backup file
4. Confirm restore

## Database Schema

### Transactions Table
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| type | TEXT | INCOME/EXPENSE |
| amount | REAL | Transaction amount |
| category | TEXT | Category name |
| description | TEXT | Optional description |
| date | INTEGER | Timestamp (ms) |
| created_at | INTEGER | Creation timestamp |

### Budgets Table
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| category | TEXT | Category name |
| monthly_budget | REAL | Budget amount |
| month | TEXT | YYYY-MM format |
| spent | REAL | Amount spent |
| created_at | INTEGER | Creation timestamp |

### Import History Table
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| file_name | TEXT | Imported file name |
| import_date | INTEGER | Import timestamp |
| transaction_count | INTEGER | Number of transactions |
| status | TEXT | SUCCESS/PARTIAL/FAILED |

## Architecture Overview

### Domain Layer
- **Models**: Transaction, Budget, Category, etc.
- **Repository Interfaces**: Contracts for data operations
- **Use Cases**: Business logic operations

### Data Layer
- **Database**: SQLite implementation
- **Repositories**: Data access implementations
- **Importers**: CSV parsing and import
- **Backup**: JSON/CSV backup management

### Presentation Layer
- **Activities**: UI screens
- **ViewModels**: UI state management
- **Adapters**: RecyclerView adapters

## Troubleshooting

### Build Errors

**"kotlinc not found"**
```bash
# Install Kotlin compiler
sdk install kotlin
# or download from GitHub releases
```

**"Android SDK not found"**
```bash
# Set correct SDK path
export ANDROID_SDK_ROOT=/path/to/android/sdk
```

**"aapt2 not found"**
```bash
# Ensure build-tools are installed
sdkmanager "build-tools;33.0.2"
```

### Runtime Errors

**"Permission denied"**
- Grant storage permissions when prompted
- For Android 11+, enable "All Files Access"

**"No device connected"**
- Enable USB Debugging
- Check USB connection
- Run `adb devices` to verify

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Kotlin | 1.9.20 | Language |
| Coroutines | 1.7.3 | Async operations |
| MPAndroidChart | v3.1.0 | Charts |
| AndroidX Core | 1.12.0 | Core utilities |
| AndroidX AppCompat | 1.6.1 | Compatibility |
| Material | 1.10.0 | Material Design |
| Gson | 2.10.1 | JSON parsing |

## License

This project is for educational purposes.

## Support

For issues or questions, please check the documentation or review the source code comments.

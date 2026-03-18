# Money Manager Android App - Copilot Instructions

## Project Overview

This is a **Kotlin-based Android application** for personal finance management, built using **Clean Architecture** principles. The project uses a CLI-based build system (Make) instead of Android Studio, compiling entirely from the command line.

**Key Points:**
- **Package:** `com.aminmart.moneymanager`
- **Min SDK:** 24, **Target SDK:** 33, **Build Tools:** 33.0.2
- **Build System:** Makefile with no Gradle
- **Architecture:** Clean Architecture (Domain → Data → Presentation layers)

## Build & Run Commands

All commands are run from `money-manager/` directory:

### Build
```bash
cd money-manager
make build           # Full debug build (checks deps, downloads libs, compiles, signs)
make build-release   # Release build with keystore (set KEYSTORE_PASSWORD, KEY_PASSWORD env vars)
make check-deps      # Verify tools are available
make deps            # Download all JARs to libs/
make clean           # Remove build output
make distclean       # Remove build output and all downloaded libs
```

### Test & Verify
```bash
make verify          # Verify APK signature
```

### Deploy
```bash
make install         # Install APK to connected device
make run             # Build, install, and launch app
make uninstall       # Uninstall from device
make devices         # List connected devices
```

### Debugging
```bash
make log             # View app logcat (tagged by app ID)
make log-filter      # Filtered logcat for money/transaction/budget keywords
make pull-db         # Extract moneymanager.db from device
make backup-data     # Backup app data
make restore-data    # Restore app data
```

## Project Structure

### Core Layers (Clean Architecture)

**Domain Layer** (`src/domain/`)
- Models: `Transaction`, `Budget`, `Category`, `ImportHistory`, `DashboardStats`
- Repository interfaces: Define contracts for data operations
- Use Cases: Business logic (e.g., `ExportUseCases`, transaction operations)
- Pure Kotlin, no Android dependencies

**Data Layer** (`src/data/`)
- `database/MoneyDatabase`: SQLite wrapper (manages transactions, budgets, import history)
- `repository/`: Implements domain repository interfaces
- `importer/CsvImporter`: Parses and imports bank statements
- `backup/BackupManager`: JSON/CSV backup and restore
- `datasource/ExportManager`: Export transactions to CSV/Excel

**Presentation Layer** (`src/presentation/`)
- `activities/`: UI screens (MainActivity, transaction screens, budget screens)
- `viewmodels/`: State management using coroutines
- `adapters/`: RecyclerView adapters for lists
- `ui/`: UI utilities and helpers

### Resource Files (`res/`)
- `layout/`: XML layouts for screens
- `values/`: Strings, colors, themes (strings.xml)
- `drawable/`: Vector icons and shape definitions
- `menu/`: Menu definitions

### Key Configuration Files
- `AndroidManifest.xml`: App manifest with permissions, activities
- `build.config`: Build settings (SDK versions, app ID, version info)
- `Makefile`: Complete build pipeline

## Database Schema

**Transactions table:** id, type (INCOME/EXPENSE), amount, category, description, date (ms timestamp), created_at
**Budgets table:** id, category, monthly_budget, month (YYYY-MM), spent, created_at
**ImportHistory table:** id, file_name, import_date, transaction_count, status (SUCCESS/PARTIAL/FAILED)

## Key Conventions

### Kotlin/Android Conventions
- Package structure mirrors Clean Architecture: `com.aminmart.moneymanager.{domain|data|presentation}.*`
- Coroutines used for async operations (not RxJava)
- Data classes for models in domain layer
- Repository pattern: interfaces in domain, implementations in data
- ViewModel inherits from `android.arch.lifecycle.ViewModel`
- Activities use ViewModels for state management

### Code Patterns
- **CSV Import:** CsvImporter parses CSV, detects duplicates, returns CsvImportResult with success/partial/failed status
- **Backup:** Automatic on app close; manual via BackupManager; stores to Documents/MoneyManagerBackup/
- **Export:** ExportUseCases handles CSV/Excel export filtered by date range
- **Categories:** Predefined category list; transactions auto-categorized or user-selected

### Dependencies
Located in `libs/`: Kotlin stdlib, Coroutines, MPAndroidChart (charting), AndroidX (core/appcompat/recyclerview), Material Design, Gson
Downloaded at build time via Makefile targets using Maven/JCenter URLs.

## Build System Details

The Makefile orchestrates:
1. **Dependency Check:** Verifies Android SDK, Kotlin compiler, build tools
2. **Download Libs:** JARs fetched if missing
3. **Compile Resources:** `aapt2 compile` → `aapt2 link` (generates R.java)
4. **Compile Kotlin:** kotlinc with classpath pointing to android.jar + all libs
5. **DEX Conversion:** d8 converts .class to .dex
6. **Package APK:** Combines dex + resources into .apk
7. **Align:** zipalign for optimal performance
8. **Sign:** apksigner with debug key (auto-generated) or release key

**Classpath for compilation:** `platforms/android-33/android.jar:libs/*`

## Environment Setup

Requires:
- **ANDROID_SDK_ROOT** or **ANDROID_HOME:** Path to Android SDK
- **PATH:** Must include kotlinc, adb, keytool
- **Optional env vars for release builds:** KEYSTORE_PASSWORD, KEY_PASSWORD

## Common Patterns & Tips

### Adding a Feature
1. Define model in `domain/model/`
2. Create repository interface in `domain/repository/`
3. Implement repository in `data/repository/`
4. Add database operations to `MoneyDatabase` if needed
5. Create ViewModel in `presentation/viewmodels/`
6. Create or update Activity in `presentation/activities/`
7. Create adapter if displaying lists

### Modifying CSV Import
- Edit `src/data/importer/CsvImporter.kt`
- Expected format: Date, Description, Amount (see README for example)
- Duplicate detection happens during import

### Adding Database Queries
- Extend `MoneyDatabase` class
- Use `ContentValues` for inserts/updates
- Cursor for queries

### Handling Data Persistence
- Use Flow<> from coroutines for reactive data
- ViewModels observe database changes and update UI
- Repository implementations handle DB access

### Chart Display
- Uses MPAndroidChart library
- Integration in presentation layer activities
- DashboardStats model provides aggregated data

## Troubleshooting Notes

- **Build fails with missing tools:** Run `make check-deps` first
- **Device not recognized:** Verify USB debugging enabled; run `make devices`
- **APK size:** Located at `money-manager.apk` after build; check with `ls -lh`
- **Database issues:** Extract with `make pull-db` for inspection
- **Release signing:** Create keystore with `keytool`, set env vars, run `make build-release`

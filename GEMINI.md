# GEMINI.md - Money Manager Android App

## Project Overview

This is a personal finance management application for Android, written entirely in Kotlin. It follows the Clean Architecture pattern, separating concerns into `domain`, `data`, and `presentation` layers.

A unique characteristic of this project is its build system. It does **not** use Gradle or Android Studio's default build system. Instead, it relies on a `Makefile` and command-line tools like `kotlinc`, `aapt2`, and `d8` to compile, package, and sign the APK. This makes it a lightweight, CLI-focused project.

**Key Technologies & Principles:**
- **Language:** Kotlin
- **Architecture:** Clean Architecture
- **Build System:** Makefile
- **Database:** SQLite (local)
- **Concurrency:** Kotlin Coroutines
- **UI:** Android XML Layouts, Material Components
- **Dependencies:** Manually managed via `Makefile` and stored in the `libs/` directory.

## Building and Running

The project has been migrated to use **Gradle** as the primary build system, but the `Makefile` remains available as a convenient CLI wrapper. Ensure you have the Android SDK and Gradle (or use the provided wrapper if available) configured.

### Main Commands

- **Build the app (debug):**
  ```bash
  make build
  # or directly via gradle
  ./gradlew assembleDebug
  ```

- **Install and run on a connected device:**
  ```bash
  make run
  # or directly via gradle
  ./gradlew installDebug
  adb shell am start -n com.aminmart.moneymanager/com.aminmart.moneymanager.presentation.activities.MainActivity
  ```

- **Clean the project:**
  ```bash
  make clean
  # or
  ./gradlew clean
  ```

- **View logs:**
  ```bash
  make log
  ```

For a full list of commands, see the `Makefile` or run `make help`. Dependencies are now automatically managed by Gradle.

## Development Conventions

- **Project Structure:**
  - `app/src/main/java/com/aminmart/moneymanager/domain/`: Contains business logic and models (pure Kotlin).
    - `model/`: Defines core entities like `Transaction`, `Budget`.
    - `repository/`: Defines interfaces for data sources.
    - `usecase/`: Contains specific business operations.
  - `app/src/main/java/com/aminmart/moneymanager/data/`: Implements the repository interfaces and handles data sources.
    - `database/`: SQLite database setup and access.
    - `repository/`: Concrete implementations of the domain repositories.
  - `app/src/main/java/com/aminmart/moneymanager/presentation/`: Handles the UI and user interaction.
    - `activities/`: Android Activities for each screen.
    - `viewmodels/`: Manages UI-related data and state.
  - `app/src/main/res/`: Android resources (layouts, drawables, values).
  - `app/src/main/AndroidManifest.xml`: Android app manifest.

- **Adding a Feature (General Flow):**
  1.  Define the model in the `domain/model` directory.
  2.  Define the repository interface in `domain/repository`.
  3.  Implement the repository in the `data/repository` directory, including any database changes in `data/database/MoneyDatabase.kt`.
  4.  Create `UseCase`s in `domain/usecase` for the new business logic.
  5.  Create a `ViewModel` in `presentation/viewmodels` to prepare data for the UI.
  6.  Create the `Activity` and XML layout in `presentation/activities` and `res/layout`.

## Future Improvements (User Request)

The following features have been requested to enhance the application:

1.  **Utang Piutang (Debt & Credit Tracking):**
    - **Goal:** Track money owed to the user and by the user.
    - **Implementation:** Requires a new data model, database table, and dedicated UI screens.

2.  **Catatan Riba (Interest Tracking):**
    - **Goal:** Specifically flag or take notes on transactions involving interest (Riba), in line with Islamic finance principles.
    - **Implementation:** Could be a new field/tag on the `Transaction` model.

3.  **Penyederhanaan UI/UX (Simplified UI/UX):**
    - **Goal:** Make the app simpler and faster to use to encourage frequent data entry.
    - **Focus:** Improve the user flow for adding transactions and reduce friction.

4.  **Rencana Keuangan (Financial Planning):**
    - **Goal:** Add capabilities for long-term financial planning beyond monthly budgeting.
    - **Implementation:** Could involve setting financial goals (e.g., saving for a large purchase) and tracking progress.

5.  **Verifikasi Fitur (Existing Feature Verification):**
    - **Backup/Import:** The functionality exists but should be reviewed for robustness.
    - **Offline-Only:** The app is already offline-only; this should be maintained.

This `GEMINI.md` file will serve as a guide for these future development tasks.

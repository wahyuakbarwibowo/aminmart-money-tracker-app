# Repository Guidelines

## Project Structure & Module Organization
This repository is a Kotlin Android app using a Clean Architecture split under `app/src/main/java/com/aminmart/moneymanager/`:
- `domain/`: models, repository interfaces, and use cases.
- `data/`: database, repository implementations, import/export, and backup logic.
- `presentation/`: `activities/`, `viewmodels/`, and RecyclerView `adapters/`.
- `app/src/main/res/`: layouts, drawables, menus, and values.
Build scripts live at the repo root (`Makefile`, `build.gradle.kts`, `settings.gradle.kts`) and module config in `app/build.gradle.kts`.

## Build, Test, and Development Commands
Use Gradle wrapper directly or Make targets.
- `rtk make build`: build debug APK (`app/build/outputs/apk/debug/app-debug.apk`).
- `rtk make debug`: alias for debug APK build.
- `rtk make build-release`: build release APK.
- `rtk make release-signed`: build signed release APK (requires keystore env vars).
- `rtk make install`: install debug APK to connected device/emulator.
- `rtk make run`: install and launch `MainActivity` via ADB.
- `rtk make log` / `rtk make log-filter`: stream app logs.
- `rtk make clean`: remove build outputs.
- `rtk ./gradlew test`: run JVM unit tests.
- `rtk ./gradlew connectedAndroidTest`: run instrumentation tests on device.
- Import feature note: CSV import is handled by `ImportCsvActivity`, while JSON import is handled via Settings restore flow.

## Coding Style & Naming Conventions
- Follow Kotlin conventions: 4-space indentation, clear null-safety handling, and small focused functions.
- Class names: `PascalCase` (e.g., `DashboardViewModel`), functions/variables: `camelCase`, constants: `UPPER_SNAKE_CASE`.
- Keep layer boundaries strict: `presentation` should call `domain` use cases, not `data` directly.
- XML resource naming should stay descriptive and snake_case (e.g., `activity_dashboard.xml`, `item_transaction.xml`).

## Testing Guidelines
- Unit tests go in `app/src/test`; Android/instrumentation tests go in `app/src/androidTest`.
- Test class naming: `<ClassName>Test`; method naming: behavior-focused (e.g., `getTotalBudget_returnsRemaining_whenExpensesExist`).
- Run `rtk ./gradlew test` before opening a PR; use `connectedAndroidTest` for UI/integration-sensitive changes.

## Commit & Pull Request Guidelines
Recent history uses Conventional Commit prefixes (`feat:`, `fix:`, `chore:`, `docs:`); continue this format.
- Keep commits scoped and atomic (one concern per commit).
- PRs should include: problem summary, solution notes, test evidence (commands run), and screenshots/video for UI changes.
- Link related issues/tasks and call out config or migration impacts explicitly.

## Security & Configuration Tips
- Do not commit secrets or signing credentials. Keep keystores out of version control.
- Store local SDK/keystore paths in environment variables, not source files.
- Validate file import/export changes carefully to avoid exposing user financial data.

# Money Manager Android App - Makefile (Gradle Wrapper)
# This Makefile now wraps Gradle commands for easier CLI usage.

# Load build configuration for APP_ID and other variables
include build.config

# Gradle executable
# If gradlew doesn't exist, try 'gradle'
GRADLE := $(shell if [ -f ./gradlew ]; then echo ./gradlew; else echo gradle; fi)

.PHONY: all
all: build

# Main build target (debug)
.PHONY: build debug
build debug:
	$(GRADLE) assembleDebug
	@echo ""
	@echo "=========================================="
	@echo "BUILD SUCCESSFUL"
	@echo "=========================================="
	@echo "APK: app/build/outputs/apk/debug/app-debug.apk"
	@echo ""

# Build signed release APK
.PHONY: build-release
build-release:
	$(GRADLE) assembleRelease
	@echo ""
	@echo "=========================================="
	@echo "RELEASE BUILD SUCCESSFUL"
	@echo "=========================================="
	@echo "APK: app/build/outputs/apk/release/app-release.apk"
	@echo ""

# Build signed release APK using keystore env vars
.PHONY: release-signed
release-signed:
	@test -n "$(KEYSTORE_PATH)" || (echo "ERROR: KEYSTORE_PATH is required"; exit 1)
	@test -n "$(KEYSTORE_PASSWORD)" || (echo "ERROR: KEYSTORE_PASSWORD is required"; exit 1)
	@test -n "$(KEY_ALIAS)" || (echo "ERROR: KEY_ALIAS is required"; exit 1)
	@test -n "$(KEY_PASSWORD)" || (echo "ERROR: KEY_PASSWORD is required"; exit 1)
	$(GRADLE) assembleRelease \
		-Pandroid.injected.signing.store.file="$(KEYSTORE_PATH)" \
		-Pandroid.injected.signing.store.password="$(KEYSTORE_PASSWORD)" \
		-Pandroid.injected.signing.key.alias="$(KEY_ALIAS)" \
		-Pandroid.injected.signing.key.password="$(KEY_PASSWORD)"
	@echo ""
	@echo "=========================================="
	@echo "SIGNED RELEASE BUILD SUCCESSFUL"
	@echo "=========================================="
	@echo "APK: app/build/outputs/apk/release/app-release.apk"
	@echo ""

# Install APK to device
.PHONY: install
install:
	$(GRADLE) installDebug
	@echo "Installation complete"

# Run the app
.PHONY: run
run: install
	@echo "Starting application..."
	@adb shell am start -n $(APP_ID)/.presentation.activities.MainActivity
	@echo "Application started"

# View logs
.PHONY: log
log:
	@echo "Showing logcat (Ctrl+C to stop)..."
	@adb logcat -s $(APP_ID)

# View logs with filter
.PHONY: log-filter
log-filter:
	@echo "Showing filtered logcat (Ctrl+C to stop)..."
	@adb logcat | grep -i "money\|transaction\|budget"

# Uninstall app
.PHONY: uninstall
uninstall:
	$(GRADLE) uninstallAll
	@echo "Application uninstalled"

# Clean build files
.PHONY: clean
clean:
	$(GRADLE) clean
	@echo "Clean complete"

# Download dependencies (now handled by Gradle)
.PHONY: deps
deps:
	@echo "Dependencies are now automatically managed by Gradle."
	@echo "They will be downloaded when you run 'make build'."

# Show help
.PHONY: help
help:
	@echo "Money Manager - Gradle Build System"
	@echo ""
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@echo "  debug         Build the APK (debug)"
	@echo "  build         Build the APK (debug)"
	@echo "  build-release Build the APK (release)"
	@echo "  release-signed Build signed release APK (requires keystore env vars)"
	@echo "  install       Install debug APK to device"
	@echo "  run           Build, install and run the app"
	@echo "  log           View logcat"
	@echo "  log-filter    View filtered logcat"
	@echo "  uninstall     Uninstall the app from device"
	@echo "  clean         Remove build files"
	@echo "  deps          Information about dependencies"
	@echo "  help          Show this help message"
	@echo ""

# List connected devices
.PHONY: devices
devices:
	@adb devices

# Pull app database
.PHONY: pull-db
pull-db:
	@echo "Pulling database from device..."
	@mkdir -p build
	@adb shell "run-as $(APP_ID) cat databases/moneymanager.db" > build/moneymanager.db 2>/dev/null || \
	adb pull /data/data/$(APP_ID)/databases/moneymanager.db build/moneymanager.db
	@echo "Database pulled to build/moneymanager.db"

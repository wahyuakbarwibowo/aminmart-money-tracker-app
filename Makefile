# Money Manager Android App - Makefile
# Build system for CLI-based Android development
# Requires: Android SDK, Kotlin compiler, aapt2, d8, zipalign, apksigner, adb

# Load build configuration
include build.config

# Android SDK Path (adjust if needed)
ANDROID_SDK_ROOT ?= $(HOME)/Android/Sdk
ANDROID_HOME ?= $(ANDROID_SDK_ROOT)

# Build tools path
BUILD_TOOLS_PATH = $(ANDROID_HOME)/build-tools/$(BUILD_TOOLS_VERSION)
PLATFORMS_PATH = $(ANDROID_HOME)/platforms/android-$(COMPILE_SDK_VERSION)

# Tools
AAPT2 = $(BUILD_TOOLS_PATH)/aapt2
D8 = $(BUILD_TOOLS_PATH)/d8
ZIPALIGN = $(BUILD_TOOLS_PATH)/zipalign
APKSIGNER = $(BUILD_TOOLS_PATH)/apksigner
KOTLINC = kotlinc
ADB = adb

# Paths
SRC_DIR = src
RES_DIR = res
BUILD_DIR = build
LIBS_DIR = libs

# Intermediate directories
CLASSES_DIR = $(BUILD_DIR)/classes
RES_COMPILED_DIR = $(BUILD_DIR)/res-compiled
MERGED_DIR = $(BUILD_DIR)/merged

# Output
APK_UNSIGNED = $(BUILD_DIR)/app-unsigned.apk
APK_ALIGNED = $(BUILD_DIR)/app-aligned.apk
APK_OUTPUT = $(OUTPUT_APK)
APK_SIGNED_OUTPUT = $(OUTPUT_APK_SIGNED)

# Source files
KOTLIN_SOURCES = $(shell find $(SRC_DIR) -name "*.kt")
LAYOUT_FILES = $(shell find $(RES_DIR)/layout -name "*.xml" 2>/dev/null)
VALUES_FILES = $(shell find $(RES_DIR)/values -name "*.xml" 2>/dev/null)
DRAWABLE_FILES = $(shell find $(RES_DIR)/drawable -name "*.xml" -o -name "*.png" 2>/dev/null)
MANIFEST = AndroidManifest.xml

# Libraries (download these to libs/ directory)
KOTLIN_STDLIB = $(LIBS_DIR)/kotlin-stdlib-$(KOTLIN_VERSION).jar
KOTLIN_COROUTINES = $(LIBS_DIR)/kotlinx-coroutines-android-1.7.3.jar
MPANDROIDCHART = $(LIBS_DIR)/MPAndroidChart-v3.1.0.jar
ANDROIDX_CORE = $(LIBS_DIR)/androidx-core-core-1.12.0.jar
ANDROIDX_APPCOMPAT = $(LIBS_DIR)/androidx-appcompat-appcompat-1.6.1.jar
ANDROIDX_RECYCLERVIEW = $(LIBS_DIR)/androidx-recyclerview-recyclerview-1.3.1.jar
ANDROIDX_MATERIAL = $(LIBS_DIR)/material-1.10.0.jar
GSON = $(LIBS_DIR)/gson-2.10.1.jar

# All libraries
ALL_LIBS = $(KOTLIN_STDLIB) $(KOTLIN_COROUTINES) $(MPANDROIDCHART) $(ANDROIDX_CORE) \
           $(ANDROIDX_APPCOMPAT) $(ANDROIDX_RECYCLERVIEW) $(ANDROIDX_MATERIAL) $(GSON)

# Classpath for compilation
CLASSPATH = $(PLATFORMS_PATH)/android.jar:$(LIBS_DIR)/*

# Default target
.PHONY: all
all: build

# Check dependencies
.PHONY: check-deps
check-deps:
	@echo "Checking dependencies..."
	@if [ ! -d "$(ANDROID_HOME)" ]; then \
		echo "ERROR: Android SDK not found at $(ANDROID_HOME)"; \
		echo "Please set ANDROID_SDK_ROOT or ANDROID_HOME environment variable"; \
		exit 1; \
	fi
	@if ! command -v $(KOTLINC) >/dev/null 2>&1; then \
		echo "ERROR: kotlinc not found. Please install Kotlin compiler."; \
		echo "Download from: https://github.com/JetBrains/kotlin/releases"; \
		exit 1; \
	fi
	@if [ ! -f "$(AAPT2)" ]; then \
		echo "ERROR: aapt2 not found at $(AAPT2)"; \
		exit 1; \
	fi
	@echo "All dependencies OK"

# Download libraries
.PHONY: deps
deps: $(ALL_LIBS)

$(LIBS_DIR)/%.jar:
	@echo "Downloading $@..."
	@mkdir -p $(LIBS_DIR)
	@case "$@" in \
		*kotlin-stdlib*) \
			curl -L -o "$@" "https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/$(KOTLIN_VERSION)/kotlin-stdlib-$(KOTLIN_VERSION).jar" ;; \
		*kotlinx-coroutines*) \
			curl -L -o "$@" "https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-android/1.7.3/kotlinx-coroutines-android-1.7.3.jar" ;; \
		*MPAndroidChart*) \
			curl -L -o "$@" "https://jitpack.io/com/github/PhilJay/MPAndroidChart/v3.1.0/MPAndroidChart-v3.1.0.jar" ;; \
		*androidx-core*) \
			curl -L -o "$@" "https://repo1.maven.org/maven2/androidx/core/core-ktx/1.12.0/core-ktx-1.12.0.jar" ;; \
		*androidx-appcompat*) \
			curl -L -o "$@" "https://repo1.maven.org/maven2/androidx/appcompat/appcompat/1.6.1/appcompat-1.6.1.jar" ;; \
		*androidx-recyclerview*) \
			curl -L -o "$@" "https://repo1.maven.org/maven2/androidx/recyclerview/recyclerview/1.3.1/recyclerview-1.3.1.jar" ;; \
		*material*) \
			curl -L -o "$@" "https://repo1.maven.org/maven2/com/google/android/material/material/1.10.0/material-1.10.0.jar" ;; \
		*gson*) \
			curl -L -o "$@" "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar" ;; \
	esac
	@if [ ! -f "$@" ]; then \
		echo "Failed to download $@"; \
		exit 1; \
	fi
	@echo "Downloaded $@"

# Compile resources
$(RES_COMPILED_DIR): $(RES_DIR) $(MANIFEST)
	@echo "Compiling resources..."
	@mkdir -p $(RES_COMPILED_DIR)
	$(AAPT2) compile --dir $(RES_DIR) -o $(RES_COMPILED_DIR)/res.zip
	@mkdir -p $(RES_COMPILED_DIR)/flat
	@unzip -o $(RES_COMPILED_DIR)/res.zip -d $(RES_COMPILED_DIR)/flat
	@echo "Resources compiled"

# Link resources and manifest
$(MERGED_DIR)/resources.apk: $(RES_COMPILED_DIR) $(MANIFEST)
	@echo "Linking resources..."
	@mkdir -p $(MERGED_DIR)
	$(AAPT2) link \
		-o $@ \
		--manifest $(MANIFEST) \
		-R $(PLATFORMS_PATH)/android.jar \
		-I $(PLATFORMS_PATH)/android.jar \
		$(RES_COMPILED_DIR)/res.zip \
		--java $(BUILD_DIR)/generated \
		--min-sdk-version $(MIN_SDK_VERSION) \
		--target-sdk-version $(TARGET_SDK_VERSION) \
		--version-code $(VERSION_CODE) \
		--version-name $(VERSION_NAME) \
		--package-id 127 \
		--allow-reserved-package-id
	@echo "Resources linked"

# Compile Kotlin sources
$(CLASSES_DIR): $(KOTLIN_SOURCES) $(ALL_LIBS) $(MERGED_DIR)/resources.apk
	@echo "Compiling Kotlin sources..."
	@mkdir -p $(CLASSES_DIR)
	$(KOTLINC) $(KOTLIN_SOURCES) \
		-classpath $(CLASSPATH) \
		-d $(CLASSES_DIR) \
		-no-stdlib \
		-Xallow-result-return-type \
		-jvm-target 1.8 \
		-Xno-param-assertions \
		-Xno-call-assertions \
		-Xno-receiver-assertions
	@echo "Kotlin compilation complete"

# Compile to DEX
$(BUILD_DIR)/classes.dex: $(CLASSES_DIR) $(MERGED_DIR)/resources.apk
	@echo "Converting to DEX..."
	$(D8) \
		--classpath $(CLASSPATH) \
		--output $(BUILD_DIR) \
		$(CLASSES_DIR)/**/*.class \
		$(CLASSES_DIR)/*.class
	@echo "DEX conversion complete"

# Build unsigned APK
$(APK_UNSIGNED): $(BUILD_DIR)/classes.dex $(MERGED_DIR)/resources.apk
	@echo "Building unsigned APK..."
	@mkdir -p $(BUILD_DIR)/apk
	@cp $(BUILD_DIR)/classes.dex $(BUILD_DIR)/apk/
	@cp $(MERGED_DIR)/resources.apk $(BUILD_DIR)/apk/resources.apk
	@cd $(BUILD_DIR)/apk && unzip -o resources.apk
	@rm $(BUILD_DIR)/apk/resources.apk
	@cd $(BUILD_DIR)/apk && zip -r ../app-unsigned.apk .
	@echo "Unsigned APK built"

# Align APK
$(APK_ALIGNED): $(APK_UNSIGNED)
	@echo "Aligning APK..."
	$(ZIPALIGN) -f -v 4 $(APK_UNSIGNED) $(APK_ALIGNED)
	@echo "APK aligned"

# Sign APK (debug key)
$(APK_OUTPUT): $(APK_ALIGNED)
	@echo "Signing APK..."
	@# Generate debug keystore if not exists
	@if [ ! -f $(BUILD_DIR)/debug.keystore ]; then \
		keytool -genkey -v -keystore $(BUILD_DIR)/debug.keystore \
			-alias androiddebugkey -storepass android \
			-keypass android -keyalg RSA -keysize 2048 \
			-validity 10000 -dname "CN=Android Debug,O=Android,C=US" \
			-storetype pkcs12 2>/dev/null || \
		keytool -genkeypair -v -keystore $(BUILD_DIR)/debug.keystore \
			-alias androiddebugkey -storepass android \
			-keypass android -keyalg RSA -keysize 2048 \
			-validity 10000 -dname "CN=Android Debug,O=Android,C=US" \
			-storetype pkcs12; \
	fi
	$(APKSIGNER) sign \
		--ks $(BUILD_DIR)/debug.keystore \
		--ks-pass pass:android \
		--ks-key-alias androiddebugkey \
		--key-pass pass:android \
		--out $(APK_OUTPUT) \
		$(APK_ALIGNED)
	@echo "APK signed: $(APK_OUTPUT)"

# Build signed APK with release key (optional)
$(APK_SIGNED_OUTPUT): $(APK_ALIGNED)
	@echo "Signing APK with release key..."
	@if [ ! -f $(BUILD_DIR)/release.keystore ]; then \
		echo "ERROR: release.keystore not found"; \
		echo "Create one with: keytool -genkey -v -keystore $(BUILD_DIR)/release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000"; \
		exit 1; \
	fi
	$(APKSIGNER) sign \
		--ks $(BUILD_DIR)/release.keystore \
		--ks-pass pass:$(KEYSTORE_PASSWORD) \
		--ks-key-alias release \
		--key-pass pass:$(KEY_PASSWORD) \
		--out $(APK_SIGNED_OUTPUT) \
		$(APK_ALIGNED)
	@echo "Release APK signed: $(APK_SIGNED_OUTPUT)"

# Main build target (debug)
.PHONY: build
build: check-deps deps $(APK_OUTPUT)
	@echo ""
	@echo "=========================================="
	@echo "BUILD SUCCESSFUL"
	@echo "=========================================="
	@echo "APK: $(APK_OUTPUT)"
	@echo "Size: $$(ls -lh $(APK_OUTPUT) | awk '{print $$5}')"
	@echo ""

# Build signed release APK
.PHONY: build-release
build-release: check-deps deps $(APK_SIGNED_OUTPUT)
	@echo ""
	@echo "=========================================="
	@echo "RELEASE BUILD SUCCESSFUL"
	@echo "=========================================="
	@echo "APK: $(APK_SIGNED_OUTPUT)"
	@echo "Size: $$(ls -lh $(APK_SIGNED_OUTPUT) | awk '{print $$5}')"
	@echo ""
	@echo "Remember to backup your keystore!"
	@echo ""

# Install APK to device
.PHONY: install
install: $(APK_OUTPUT)
	@echo "Installing APK to device..."
	@$(ADB) devices | grep -q "device$$" || (echo "ERROR: No device connected. Connect a device via USB and enable USB debugging." && exit 1)
	@$(ADB) install -r $(APK_OUTPUT)
	@echo "Installation complete"

# Run the app
.PHONY: run
run: install
	@echo "Starting application..."
	@$(ADB) shell am start -n $(APP_ID)/.presentation.activities.MainActivity
	@echo "Application started"

# View logs
.PHONY: log
log:
	@echo "Showing logcat (Ctrl+C to stop)..."
	@$(ADB) logcat -s $(APP_ID)

# View logs with filter
.PHONY: log-filter
log-filter:
	@echo "Showing filtered logcat (Ctrl+C to stop)..."
	@$(ADB) logcat | grep -i "money\|transaction\|budget"

# Uninstall app
.PHONY: uninstall
uninstall:
	@echo "Uninstalling application..."
	@$(ADB) uninstall $(APP_ID) || true
	@echo "Application uninstalled"

# Clean build files
.PHONY: clean
clean:
	@echo "Cleaning build files..."
	@rm -rf $(BUILD_DIR)
	@rm -f $(APK_OUTPUT) $(APK_SIGNED_OUTPUT)
	@echo "Clean complete"

# Clean all including libs
.PHONY: distclean
distclean: clean
	@echo "Cleaning all files..."
	@rm -rf $(LIBS_DIR)/*.jar
	@echo "Distclean complete"

# Show help
.PHONY: help
help:
	@echo "Money Manager - Build System"
	@echo ""
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@echo "  build         Build the APK (default, debug signed)"
	@echo "  build-release Build signed release APK (requires release.keystore)"
	@echo "  install       Install APK to connected device"
	@echo "  run           Build, install and run the app"
	@echo "  log           View logcat"
	@echo "  log-filter    View filtered logcat"
	@echo "  uninstall     Uninstall the app from device"
	@echo "  clean         Remove build files"
	@echo "  distclean     Remove build files and downloaded libs"
	@echo "  deps          Download all dependencies"
	@echo "  check-deps    Check if all required tools are available"
	@echo "  verify        Verify APK signature"
	@echo "  devices       List connected devices"
	@echo "  help          Show this help message"
	@echo ""
	@echo "Environment Variables:"
	@echo "  ANDROID_SDK_ROOT  Path to Android SDK (default: ~/Android/Sdk)"
	@echo "  ANDROID_HOME      Alternative path to Android SDK"
	@echo "  KEYSTORE_PASSWORD Password for release keystore (for build-release)"
	@echo "  KEY_PASSWORD      Password for release key (for build-release)"
	@echo ""
	@echo "Requirements:"
	@echo "  - Android SDK (API 33)"
	@echo "  - Build Tools 33.0.2"
	@echo "  - Kotlin Compiler (kotlinc)"
	@echo "  - adb (Android Debug Bridge)"
	@echo "  - keytool (for keystore generation)"
	@echo ""
	@echo "Release Build:"
	@echo "  1. Generate keystore: keytool -genkey -v -keystore build/release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000"
	@echo "  2. Set passwords: export KEYSTORE_PASSWORD=xxx && export KEY_PASSWORD=xxx"
	@echo "  3. Build: make build-release"
	@echo ""

# Verify APK
.PHONY: verify
verify: $(APK_OUTPUT)
	@echo "Verifying APK..."
	$(APKSIGNER) verify $(APK_OUTPUT)
	@echo "APK verification complete"

# List connected devices
.PHONY: devices
devices:
	@echo "Connected devices:"
	@$(ADB) devices

# Backup app data
.PHONY: backup-data
backup-data:
	@echo "Backing up app data..."
	@$(ADB) backup -noapk $(APP_ID) -f $(BUILD_DIR)/backup.ab
	@echo "Backup saved to $(BUILD_DIR)/backup.ab"

# Restore app data
.PHONY: restore-data
restore-data:
	@echo "Restoring app data..."
	@$(ADB) restore $(BUILD_DIR)/backup.ab
	@echo "Restore complete"

# Pull app database
.PHONY: pull-db
pull-db:
	@echo "Pulling database from device..."
	@$(ADB) shell "run-as $(APP_ID) cat databases/moneymanager.db" > $(BUILD_DIR)/moneymanager.db 2>/dev/null || \
	$(ADB) pull /data/data/$(APP_ID)/databases/moneymanager.db $(BUILD_DIR)/moneymanager.db
	@echo "Database pulled to $(BUILD_DIR)/moneymanager.db"

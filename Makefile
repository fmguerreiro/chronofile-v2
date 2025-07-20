MAKEFLAGS += --silent

.PHONY: apk
apk:
	# Gradle requires Java 17+
	PATH="/opt/android-studio/jbr/bin:${PATH}" ./gradlew assembleRelease
	find app -name '*.apk' | grep release

.PHONY: log
log:
	adb logcat -s -v time,color AndroidRuntime Chronofile \
	  | sed -uE 's/ .\/[A-Za-z]+\([0-9]+\)://' \
	  | sed -uE 's/[0-9]{2}-[0-9]{2} ([0-9]{2}:[0-9]{2}:[0-9]{2})/\1/'

.PHONY: deploy
deploy:
	# Fast deployment with daemon
	export JAVA_HOME=$$(/usr/libexec/java_home -v 17) && ./gradlew installDebug --daemon
	adb shell am start -n com.chaidarun.chronofile/.MainActivity

.PHONY: build
build:
	# Fast build with daemon
	export JAVA_HOME=$$(/usr/libexec/java_home -v 17) && ./gradlew assembleDebug --daemon

.PHONY: clean
clean:
	# Clean build cache to resolve caching issues
	export JAVA_HOME=$$(/usr/libexec/java_home -v 17) && ./gradlew clean

.PHONY: emulator
emulator:
	# Launch the optimized ChronofileFast emulator
	emulator -avd ChronofileFast -gpu host -no-boot-anim -no-audio &

.PHONY: wait-for-emulator
wait-for-emulator:
	# Wait for emulator to be ready
	adb wait-for-device
	echo "Waiting for emulator to boot..."
	adb shell 'while [[ -z $$(getprop sys.boot_completed) ]]; do sleep 1; done'
	echo "Emulator ready!"

.PHONY: emulator-watch
emulator-watch:
	# Launch emulator and watch for changes with auto-deploy
	make emulator
	make wait-for-emulator
	make watch

.PHONY: watch
watch:
	# Watch for changes and auto-deploy on successful build
	echo "Watching for changes... Press Ctrl+C to stop"
	echo "Initial deploy..."
	make deploy
	@LAST_CHECKSUM=$$(find app/src -type f \( -name "*.kt" -o -name "*.xml" -o -name "*.java" \) -exec stat -f "%m %N" {} \; | sort | shasum -a 256 | cut -d' ' -f1); \
	CHECK_COUNTER=0; \
	while true; do \
		sleep 1; \
		CHECK_COUNTER=$$((CHECK_COUNTER + 1)); \
		if [ $$CHECK_COUNTER -ge 60 ]; then \
			CHECK_COUNTER=0; \
			if ! adb shell getprop sys.boot_completed 2>/dev/null | grep -q "1"; then \
				echo "[$$(date '+%H:%M:%S')] Emulator not responding, restarting..."; \
				make restart-emulator; \
				make deploy; \
			fi; \
		fi; \
		CURRENT_CHECKSUM=$$(find app/src -type f \( -name "*.kt" -o -name "*.xml" -o -name "*.java" \) -exec stat -f "%m %N" {} \; | sort | shasum -a 256 | cut -d' ' -f1); \
		if [ "$$LAST_CHECKSUM" != "$$CURRENT_CHECKSUM" ]; then \
			LAST_CHECKSUM="$$CURRENT_CHECKSUM"; \
			echo -e "\n[$$(date '+%H:%M:%S')] Changes detected, building..."; \
			if export JAVA_HOME=$$(/usr/libexec/java_home -v 17) && ./gradlew assembleDebug --daemon; then \
				echo "[$$(date '+%H:%M:%S')] Build successful, deploying..."; \
				if export JAVA_HOME=$$(/usr/libexec/java_home -v 17) && ./gradlew installDebug --daemon; then \
					adb shell am start -n com.chaidarun.chronofile/.MainActivity && \
					echo "[$$(date '+%H:%M:%S')] Deploy complete!"; \
				else \
					echo "[$$(date '+%H:%M:%S')] Deploy failed, checking emulator..."; \
					make check-emulator; \
				fi; \
			else \
				echo "[$$(date '+%H:%M:%S')] Build failed, skipping deploy"; \
			fi; \
		fi; \
	done

.PHONY: kill-emulator
kill-emulator:
	# Kill running emulator
	pkill -f "emulator.*ChronofileFast" || true
	adb devices | grep emulator | cut -f1 | while read emulator; do adb -s $$emulator emu kill; done || true
	sleep 2

.PHONY: restart-emulator
restart-emulator:
	# Kill and restart emulator
	make kill-emulator
	make emulator
	make wait-for-emulator

.PHONY: check-emulator
check-emulator:
	# Check if emulator is responsive
	@if ! adb shell getprop sys.boot_completed 2>/dev/null | grep -q "1"; then \
		echo "Emulator not responding, restarting..."; \
		make restart-emulator; \
	else \
		echo "Emulator is responsive"; \
	fi

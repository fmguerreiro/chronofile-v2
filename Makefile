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

.PHONY: emulator
emulator:
	# Launch the ChronofileEmu emulator
	emulator -avd ChronofileEmu &

.PHONY: wait-for-emulator
wait-for-emulator:
	# Wait for emulator to be ready
	adb wait-for-device
	echo "Waiting for emulator to boot..."
	adb shell 'while [[ -z $$(getprop sys.boot_completed) ]]; do sleep 1; done'
	echo "Emulator ready!"

.PHONY: emulator-deploy
emulator-deploy:
	# Launch emulator and deploy when ready
	make emulator
	make wait-for-emulator
	make deploy

.PHONY: kill-emulator
kill-emulator:
	# Kill running emulator
	pkill -f "emulator.*ChronofileEmu" || true

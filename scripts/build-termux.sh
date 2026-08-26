#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [ -z "${PREFIX:-}" ] || [ ! -d "$PREFIX" ]; then
  echo "ERROR: script ini harus dijalankan di Termux."
  exit 1
fi

# Locate Java 21 used by Termux.
if [ -d "$PREFIX/lib/jvm/java-21-openjdk" ]; then
  export JAVA_HOME="$PREFIX/lib/jvm/java-21-openjdk"
fi

# Locate Android SDK. Prefer an explicitly configured SDK, then Termux's standard layout.
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$SDK" ] || [ ! -d "$SDK/platforms/android-35" ]; then
  SDK=""
  for candidate in "$HOME"/lib/android-sdk-*; do
    if [ -d "$candidate/platforms/android-35" ]; then SDK="$candidate"; break; fi
  done
fi
if [ -z "$SDK" ]; then
  echo "ERROR: Android SDK 35 tidak ditemukan."
  exit 1
fi

# Configure project-local build settings.
printf 'sdk.dir=%s\n' "$SDK" > local.properties
{
  echo 'android.useAndroidX=true'
  echo 'android.enableJetifier=true'
  if command -v aapt2 >/dev/null 2>&1; then
    echo "android.aapt2FromMavenOverride=$(command -v aapt2)"
  fi
  echo 'org.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8'
} > gradle.properties

# Ensure a real Gradle Wrapper exists. The original audit package deliberately did not ship the JAR.
if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
  if ! command -v gradle >/dev/null 2>&1; then
    echo "ERROR: global 'gradle' command is required once to generate the wrapper JAR."
    exit 1
  fi
  gradle :wrapper --gradle-version 8.10.2 --distribution-type bin
fi
chmod +x gradlew

# Build using the project's pinned wrapper.
./gradlew clean assembleDebug --console=plain

APK="$(find app/build/outputs/apk/debug -maxdepth 1 -type f -name '*.apk' 2>/dev/null | head -n 1 || true)"
if [ -z "$APK" ]; then
  echo "ERROR: build completed but APK output was not found."
  exit 1
fi

echo "BUILD SUCCESSFUL"
echo "APK: $ROOT/$APK"

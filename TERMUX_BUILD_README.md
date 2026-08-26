# Termux build

This package keeps Android source portable and handles Termux-specific settings in `scripts/build-termux.sh`.

From the project root in Termux run:

```bash
./scripts/build-termux.sh
```

The script:
- detects Termux Java 21 when present;
- finds an Android SDK containing `platforms/android-35`;
- writes `local.properties`;
- enables AndroidX/Jetifier;
- points Gradle at Termux `aapt2` when installed;
- creates a real Gradle 8.10.2 wrapper if the wrapper JAR is missing;
- runs `./gradlew clean assembleDebug`;
- reports the APK path on success.

The script does not store passwords, cookies, or Facebook session data.

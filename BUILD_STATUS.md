# Build Status

This package is prepared for a clean Codemagic Android debug build.

Fixed in this revision:
- Added missing `BatchState` enum.
- Replaced invalid `globalAction()` call with Android AccessibilityService `performGlobalAction()`.
- Removed Termux-only AAPT2 override from the shared `gradle.properties`.
- Unified Java/Kotlin bytecode target at JVM 17 for CI compatibility.
- Kept Termux-only AAPT2 override inside `scripts/build-termux.sh`.
- Added a Codemagic workflow using the free-compatible `mac_mini_m2` instance.

Not yet claimed final: an APK must still be produced by Codemagic and tested on-device.

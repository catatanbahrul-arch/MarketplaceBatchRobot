# Repair Notes — CodeMagic Clean Build

## Fixed in this package

- `BatchState` is now a dedicated `BatchState.kt` source file.
- Accessibility back navigation uses `performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)`.
- Removed an unused coroutines dependency.
- CodeMagic workflow targets the individual/free-plan `mac_mini_m2` machine.
- CodeMagic uses the preinstalled `gradle` command so the build does not depend on a missing `gradle-wrapper.jar`.
- Added a source verification step before compilation.
- Android SDK location is still supplied through `local.properties`.

## Important

This package must be uploaded as the new Git commit to GitHub before running CodeMagic. Do not build an older commit/branch.

The build is an unsigned debug APK intended for testing.

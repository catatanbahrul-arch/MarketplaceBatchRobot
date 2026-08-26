# Marketplace Batch Robot Source Audit

## Fixed
- Removed `MarketplaceBatchAccessibilityService.kt.tmp`.
- Database migrations now create/repair sender reservations and account ordering.
- BatchCoordinator now uses BatchPlanner order and handles missing package/start failures by skipping safely.
- Reply reservation now distinguishes reserved vs replied and releases stale reservations.
- Reply cooldown and per-account limits are enforced before send.
- Accessibility service handles login/security challenge text conservatively and fails closed.
- Accessibility service clears callbacks on destroy.
- MainActivity no longer depends on a stale service instance for correctness beyond user-triggered start/stop.
- Safe defaults remain: robot OFF, dry-run ON, marketplace required, max 100 replies/account, 3500ms cooldown.

## Not claimable without device/build validation
- Android APK compilation (SDK/toolchain unavailable here).
- Exact Facebook UI selectors/labels on the target phone.
- Multi-account simultaneous isolation; this project assumes each account has a supported, separately logged-in Android package/profile.

## Required next validation
1. Install Android SDK 35 and JDK/Gradle through Android Studio.
2. Generate a standard Gradle wrapper if `gradle/wrapper/gradle-wrapper.jar` is still missing.
3. Run `./gradlew assembleDebug`.
4. Fix any actual compiler errors.
5. Install APK and test Accessibility + dry run + one account + batch ordering + skip-on-login + reply-once.

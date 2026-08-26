# Marketplace Batch Robot

Android Accessibility-based batch processor for Marketplace conversations.

Flow:
Akun 1 → sweep → selesai → Akun 2 → sweep → ... → semua selesai → OFF.

Safe defaults:
- Robot OFF
- Dry Run ON
- Marketplace detection REQUIRED
- No Facebook password storage
- No cookie export/storage
- No CAPTCHA/login bypass
- Max 100 replies/account/batch
- Cooldown 3500ms

Important: Facebook/Messenger UI is a third-party surface and can change. The accessibility adapter therefore fails closed when Marketplace, sender, message, input, or Send/Kirim cannot be confidently detected.

See `AUDIT_REPORT.md`, `BATCH_FLOW.md`, `FINAL_SCOPE.md`, and `BUILD_STATUS.md`.


## CodeMagic build

The repository includes `codemagic.yaml` for an Android debug APK build on the individual/free macOS M2 machine. The workflow verifies the Kotlin source files and uses the machine's installed Gradle command.

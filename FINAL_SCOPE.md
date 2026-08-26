# Marketplace Batch Robot — Final Scope

## What is being built
A sequential Android batch runner. One configured Facebook/Messenger instance is processed at a time. After an account sweep becomes idle, the robot moves to the next account. After the last account, the batch stops.

## Account/session requirement
Each account must already be logged in manually. The robot stores only account labels and package names. It does not store passwords or exported cookies. Multiple simultaneous isolated sessions depend on device/OEM clone/work-profile support, but this batch model only requires one instance at a time.

## Reply invariant
For each `(accountId, senderKey)`, at most one successful auto-reply is recorded. A transactional reservation prevents duplicate sends when two UI events arrive together. A failed send releases the reservation.

## Conservative UI behavior
The Accessibility adapter fails closed unless it sees Marketplace indicators, a sender marker, a message marker, an editable field, and a Send/Kirim control. Login prompts cause the current account to be skipped instead of guessing.

## Operational limits
Default maximum replies per account is 100 per batch. Default cooldown is 3.5 seconds between recorded successful replies for an account. These are pacing/operational limits, not a mechanism to bypass platform controls.

## Validation
Pure batch-order logic is tested with `kotlinc`. Android build requires Android Studio + Android SDK/Gradle environment on the target computer; that full toolchain is not available in this runtime, so no false claim of a compiled APK is made.

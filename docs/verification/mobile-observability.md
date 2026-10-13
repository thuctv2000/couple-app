# Mobile observability verification — 13/10/2026

Environment: Xcode 27.0 (27A266a), iOS 27 Simulator, Firebase Apple SDK 12.19.2, Gradle 9.4.1.

Verified:

- Shared JVM tests cover event timing, field allowlists, private-value exclusion, retry/ACK deduplication, conflict choices, and collector failure isolation.
- Android debug APK and KMP iOS simulator framework build successfully.
- `:feature:lovecounter:iosSimulatorArm64Test` and `:shared:iosSimulatorArm64Test` pass.
- Firebase Swift packages resolve from the committed `Package.resolved` versions and checksums.
- The complete SwiftUI app builds for the arm64 iPhone 17/iOS 27 Simulator with Firebase Auth, Analytics Core, and Crashlytics linked.
- The installed app launches with `COUPLE_FIREBASE_EMULATOR=1`; no default cloud Firebase app is configured in that mode.
- Web tests, TypeScript checks, and the production build pass after the privacy copy update.

Not verified:

- Event appearance in Firebase Analytics.
- Crash report delivery or dSYM processing in Firebase Crashlytics.
- Runtime opt-in against a real project or physical device.

Those checks require configuration supplied from an existing Firebase project. No cloud project was created or modified for this verification.

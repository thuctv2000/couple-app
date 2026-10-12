# iOS native build and startup — 12/10/2026

- Xcode 27, iOS 27 simulator (iPhone 17), Debug arm64.
- Firebase SPM 12.19.2 and 13 pinned package dependencies resolved successfully. `Package.resolved` retained in Xcode project's shared SwiftPM configuration.
- First full build failed at FirebaseCounterIdentity.swift: `getIDTokenForcingRefresh` is callback-based in the installed SDK. Changed to the actual async API `getIDToken(forcingRefresh:)`, confirmed in checked-out Firebase source.
- Second full xcodebuild returned exit 0 and BUILD SUCCEEDED. Includes SwiftUI screens, Firebase native adapter and embedded KMP framework. Signing disabled only for this simulator build.
- Installed CoupleApp.app; default local-only launch stayed running and created Room schema version 3, empty counter_settings.
- Terminated that test launch, relaunched with COUPLE_FIREBASE_EMULATOR=1; process stayed running. No claim of Anonymous/network sync success from startup alone.
- Native Gradle tests: feature 14 + shared 3, no failures/errors/skips; XML files in `ios-native-tests/`. Includes a real Room native test using a unique temporary database: local save, reopen preserving immutable outbox, ACK, apply server snapshot and reopen again. This is not a UI test.
- No native screen interaction verified: Computer Use previously requested macOS Accessibility/Screen Recording permissions. Google/Apple/linking, end-to-end iOS sync, deletion and real-device/beta checks remain.
- Local logs outside repo: `work/ios-full-build-fixed.log`, `work/ios-native-tests.log`; build output `work/ios-derived-data/Build/Products/Debug-iphonesimulator/CoupleApp.app` under workspace.

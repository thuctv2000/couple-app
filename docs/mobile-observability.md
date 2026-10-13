# Mobile analytics and crash reporting

Phase 01 uses a small shared event port with native Firebase adapters. Analytics and Crashlytics collection are opt-in and disabled before SDK startup on both platforms. The local/emulator build has no existing Firebase app configuration, reports the integrations as unavailable, and sends nothing.

## Event ownership

Shared use cases emit an allowlisted `ProductAnalyticsEvent`; UI code does not construct arbitrary event properties. Native adapters only translate that payload to Firebase. Analytics failures are swallowed at the adapter boundary and cannot change the result of a save, sync, or conflict decision.

| Event | Emitted when | Allowed extra field |
|---|---|---|
| `counter_setup_completed` | First local counter transaction succeeds | none |
| `counter_updated` | Later local counter transaction succeeds | none |
| `counter_save_failed` | Validation, stale editor, concurrent save, or storage save fails | allowlisted `error_code` |
| `sync_confirmed` | At least one pending operation receives a valid ACK and the queue drains | none |
| `sync_conflict_detected` | Sync stops on a version conflict and preserves both copies | none |
| `sync_failed` | A non-retriable failure or exhausted foreground retry stops sync | allowlisted `error_code` |
| `sync_conflict_resolved` | A current conflict choice is committed | `choice=local|server` |

Every event also has random per-event `event_id` and `schema_version=1`. Events never contain names, relationship dates, time zones, Firebase UID, counter/local owner IDs, tokens, request/response bodies, or free-form exception messages. `sync_confirmed` is not emitted for an unchanged foreground pull.

## Collection controls

- Android manifest sets `firebase_analytics_collection_enabled=false`, `google_analytics_adid_collection_enabled=false`, and `firebase_crashlytics_collection_enabled=false`.
- iOS Info.plist sets `FIREBASE_ANALYTICS_COLLECTION_ENABLED=false`, `GOOGLE_ANALYTICS_IDFV_COLLECTION_ENABLED=false`, and `FirebaseCrashlyticsCollectionEnabled=false`. The SPM product is `FirebaseAnalyticsCore`, which does not add IDFA collection capability.
- Analytics and crash reports have separate switches. Both are off by default and disabled when the app has no existing Firebase configuration or uses the local emulator.
- Runtime Firebase collection overrides persist across launches. The app also persists the displayed choices locally. Enabling Crashlytics can send crash data that Firebase retained locally while collection was disabled; the UI says this before the user opts in.
- The iOS symbol upload script only runs when `Config/GoogleService-Info.plist` from an existing project is present. No local build creates a Firebase project or uploads symbols.

Firebase references: [Analytics collection on Android](https://firebase.google.com/docs/analytics/android/configure-data-collection), [Analytics collection on Apple platforms](https://firebase.google.com/docs/analytics/ios/configure-data-collection), [Crashlytics opt-in on Android](https://firebase.google.com/docs/crashlytics/android/customize-crash-reports), and [Crashlytics opt-in on Apple platforms](https://firebase.google.com/docs/crashlytics/ios/customize-crash-reports).

## Verification boundary

JVM tests verify event timing, allowlisted fields, absence of private fixture values, exactly one confirmation after retry/ACK, both conflict choices, and isolation when the collector throws. Android and KMP native builds pass; the complete SwiftUI app also builds with Xcode 27.0 and Firebase 12.19.2. The app launches on an iOS 27 simulator in local-emulator mode, where both integrations remain unavailable by design. A real Firebase dashboard, delivery, dSYM processing, and crash report remain unverified until the owner supplies configuration for an existing project and explicitly enables collection on a test device. This work never creates a Firebase cloud project.

import Foundation
@preconcurrency import FirebaseAuth
@preconcurrency import FirebaseAnalytics
@preconcurrency import FirebaseCrashlytics
@preconcurrency import FirebaseCore
@preconcurrency import CoupleShared

@MainActor
final class FirebaseObservability: NSObject, @preconcurrency ProductAnalytics {
    private let defaults = UserDefaults.standard
    let available: Bool
    private(set) var analyticsEnabled: Bool
    private(set) var crashReportsEnabled: Bool

    init(emulator: Bool) {
        available = !emulator && FirebaseApp.app() != nil
        analyticsEnabled = available && defaults.bool(forKey: "analytics_enabled")
        crashReportsEnabled = available && defaults.bool(forKey: "crash_reports_enabled")
        super.init()
        guard available else { return }
        Analytics.setAnalyticsCollectionEnabled(analyticsEnabled)
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(crashReportsEnabled)
    }

    func setAnalyticsEnabled(_ enabled: Bool) {
        analyticsEnabled = available && enabled
        defaults.set(analyticsEnabled, forKey: "analytics_enabled")
        if available { Analytics.setAnalyticsCollectionEnabled(analyticsEnabled) }
    }

    func setCrashReportsEnabled(_ enabled: Bool) {
        crashReportsEnabled = available && enabled
        defaults.set(crashReportsEnabled, forKey: "crash_reports_enabled")
        if available { Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(crashReportsEnabled) }
    }

    func track(event: ProductAnalyticsEvent) {
        guard analyticsEnabled else { return }
        var parameters: [String: String] = [:]
        for key in ["event_id", "schema_version", "error_code", "choice"] {
            if let value = event.parameterValue(name: key) { parameters[key] = value }
        }
        Analytics.logEvent(event.name, parameters: parameters)
    }
}

/// Native SDK owner. Shared session calls this bridge from its main dispatcher.
@MainActor
final class FirebaseCounterIdentity: NSObject, @preconcurrency NativeCounterIdentity {
    private let auth: Auth

    init(app: FirebaseApp, emulator: Bool) {
        auth = Auth.auth(app: app)
        super.init()
        if emulator { auth.useEmulator(withHost: "127.0.0.1", port: 19099) }
    }

    func currentUid() -> String? { auth.currentUser?.uid }

    func requestCredential(forceRefresh: Bool, completion: @escaping (NativeCounterCredential?, String?) -> Void) {
        Task { @MainActor in
            do {
                let user: User
                if let current = auth.currentUser { user = current }
                else { user = try await auth.signInAnonymously().user }
                let token = try await user.getIDToken(forcingRefresh: forceRefresh)
                guard auth.currentUser?.uid == user.uid else { completion(nil, nil); return }
                completion(NativeCounterCredential(uid: user.uid, idToken: token), nil)
            } catch {
                completion(nil, "auth_unavailable")
            }
        }
    }

    static func makeSession(analytics: ProductAnalytics) -> CounterSession {
        #if DEBUG && targetEnvironment(simulator)
        if ProcessInfo.processInfo.environment["COUPLE_FIREBASE_EMULATOR"] == "1" {
            let name = "couple-demo-emulator"
            if FirebaseApp.app(name: name) == nil {
                let options = FirebaseOptions(googleAppID: "1:1234567890:ios:1234567890abcdef", gcmSenderID: "1234567890")
                options.projectID = "demo-couple-app"
                options.apiKey = "AIza" + String(repeating: "0", count: 35)
                FirebaseApp.configure(name: name, options: options)
            }
            if let app = FirebaseApp.app(name: name) {
                let identity = FirebaseCounterIdentity(app: app, emulator: true)
                return CounterSessionFactoryKt.createSyncedCounterSession(identity: identity, apiBaseUrl: "http://127.0.0.1:18080", allowLocalHttp: true, analytics: analytics)
            }
        }
        #endif
        return CounterSessionFactoryKt.createCounterSession(analytics: analytics)
    }

    static var usesLocalEmulator: Bool {
        #if DEBUG && targetEnvironment(simulator)
        return ProcessInfo.processInfo.environment["COUPLE_FIREBASE_EMULATOR"] == "1"
        #else
        return false
        #endif
    }
}

import Foundation
@preconcurrency import FirebaseAuth
@preconcurrency import FirebaseCore
@preconcurrency import CoupleShared

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

    static func makeSession() -> CounterSession {
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
                return CounterSessionFactoryKt.createSyncedCounterSession(identity: identity, apiBaseUrl: "http://127.0.0.1:18080", allowLocalHttp: true)
            }
        }
        #endif
        return CounterSessionFactoryKt.createCounterSession()
    }
}

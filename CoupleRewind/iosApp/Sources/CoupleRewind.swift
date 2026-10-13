import SwiftUI
@preconcurrency import FirebaseCore

@main
struct CoupleRewind: App {
    init() {
        #if DEBUG && targetEnvironment(simulator)
        let usesLocalEmulator = ProcessInfo.processInfo.environment["COUPLE_FIREBASE_EMULATOR"] == "1"
        #else
        let usesLocalEmulator = false
        #endif
        if !usesLocalEmulator,
           FirebaseApp.app() == nil,
           Bundle.main.url(forResource: "GoogleService-Info", withExtension: "plist") != nil {
            FirebaseApp.configure()
        }
    }

    var body: some Scene {
        WindowGroup { LoveCounterView() }
    }
}

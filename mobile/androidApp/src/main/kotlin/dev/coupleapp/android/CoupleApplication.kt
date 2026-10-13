package dev.coupleapp.android
import android.app.Application
import dev.coupleapp.shared.createCounterSession
class CoupleApplication : Application() {
    val observability by lazy { FirebaseObservability(this, emulator = BuildConfig.USE_FIREBASE_EMULATOR) }
    val counterSession by lazy {
        createCounterSession(this, emulator = BuildConfig.USE_FIREBASE_EMULATOR, analytics = observability)
    }
}

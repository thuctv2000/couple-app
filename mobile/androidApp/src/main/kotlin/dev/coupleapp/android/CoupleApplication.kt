package dev.coupleapp.android
import android.app.Application
import dev.coupleapp.shared.createCounterSession
class CoupleApplication : Application() {
    val counterSession by lazy { createCounterSession(this, emulator = BuildConfig.USE_FIREBASE_EMULATOR) }
}

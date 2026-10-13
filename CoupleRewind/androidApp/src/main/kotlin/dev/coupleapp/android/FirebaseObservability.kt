package dev.coupleapp.android

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dev.coupleapp.shared.observability.ProductAnalytics
import dev.coupleapp.shared.observability.ProductAnalyticsEvent

/** Firebase collection remains off until the user enables each category. */
class FirebaseObservability(context: Context, emulator: Boolean) : ProductAnalytics {
    private val applicationContext = context.applicationContext
    private val preferences = applicationContext.getSharedPreferences("observability_preferences", Context.MODE_PRIVATE)
    private val firebaseConfigured = applicationContext.resources.getIdentifier(
        "google_app_id",
        "string",
        applicationContext.packageName,
    ) != 0
    val available: Boolean = !emulator && firebaseConfigured

    var analyticsEnabled: Boolean
        private set
    var crashReportsEnabled: Boolean
        private set

    init {
        analyticsEnabled = available && preferences.getBoolean(KEY_ANALYTICS, false)
        crashReportsEnabled = available && preferences.getBoolean(KEY_CRASH_REPORTS, false)
        applyCollectionSettings()
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        analyticsEnabled = available && enabled
        preferences.edit().putBoolean(KEY_ANALYTICS, analyticsEnabled).apply()
        if (available) runCatching { FirebaseAnalytics.getInstance(applicationContext).setAnalyticsCollectionEnabled(analyticsEnabled) }
    }

    fun setCrashReportsEnabled(enabled: Boolean) {
        crashReportsEnabled = available && enabled
        preferences.edit().putBoolean(KEY_CRASH_REPORTS, crashReportsEnabled).apply()
        if (available) runCatching { FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(crashReportsEnabled) }
    }

    override fun track(event: ProductAnalyticsEvent) {
        if (!analyticsEnabled) return
        runCatching {
            val parameters = Bundle().apply {
                event.parameters.forEach { (key, value) -> putString(key, value) }
            }
            FirebaseAnalytics.getInstance(applicationContext).logEvent(event.name, parameters)
        }
    }

    private fun applyCollectionSettings() {
        if (!firebaseConfigured) return
        runCatching { FirebaseAnalytics.getInstance(applicationContext).setAnalyticsCollectionEnabled(analyticsEnabled) }
        runCatching { FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(crashReportsEnabled) }
    }

    private companion object {
        const val KEY_ANALYTICS = "analytics_enabled"
        const val KEY_CRASH_REPORTS = "crash_reports_enabled"
    }
}

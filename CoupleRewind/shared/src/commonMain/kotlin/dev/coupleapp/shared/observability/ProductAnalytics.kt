package dev.coupleapp.shared.observability

import kotlin.random.Random

/**
 * Product events are constructed inside shared use cases. Hosts receive an allowlisted payload,
 * so relationship content, dates, account IDs and credentials cannot be attached by UI code.
 */
class ProductAnalyticsEvent internal constructor(
    val name: String,
    val parameters: Map<String, String>,
) {
    fun parameterValue(name: String): String? = parameters[name]
}

fun interface ProductAnalytics {
    fun track(event: ProductAnalyticsEvent)
}

object DisabledProductAnalytics : ProductAnalytics {
    override fun track(event: ProductAnalyticsEvent) = Unit
}

internal object ProductEvents {
    fun counterSaved(initial: Boolean) = event(if (initial) "counter_setup_completed" else "counter_updated")

    fun counterSaveFailed(code: String) = event(
        name = "counter_save_failed",
        values = mapOf("error_code" to allowedSaveError(code)),
    )

    fun syncConfirmed() = event("sync_confirmed")

    fun syncFailed(code: String) = event(
        name = if (code == "conflict") "sync_conflict_detected" else "sync_failed",
        values = if (code == "conflict") emptyMap() else mapOf("error_code" to allowedSyncError(code)),
    )

    fun conflictResolved(keepLocal: Boolean) = event(
        name = "sync_conflict_resolved",
        values = mapOf("choice" to if (keepLocal) "local" else "server"),
    )

    private fun event(name: String, values: Map<String, String> = emptyMap()): ProductAnalyticsEvent {
        return ProductAnalyticsEvent(
            name = name,
            parameters = buildMap {
                put("event_id", randomEventId())
                put("schema_version", "1")
                putAll(values)
            },
        )
    }

    private fun allowedSaveError(code: String) = when (code) {
        "invalid_date", "future_date", "invalid_time_zone", "invalid_input", "edit_changed", "save_in_progress" -> code
        else -> "save_failed"
    }

    private fun allowedSyncError(code: String) = when (code) {
        "auth_required", "account_conflict", "forbidden", "invalid_operation", "invalid_response",
        "retry_later", "storage_unavailable" -> code
        else -> "sync_error"
    }

    private fun randomEventId(): String = Random.nextBytes(16).joinToString(separator = "") { byte ->
        byte.toUByte().toString(16).padStart(2, '0')
    }
}

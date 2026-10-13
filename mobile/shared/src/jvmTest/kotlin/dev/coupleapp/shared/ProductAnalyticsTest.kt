package dev.coupleapp.shared

import dev.coupleapp.lovecounter.domain.CounterDraft
import dev.coupleapp.lovecounter.domain.CounterRepository
import dev.coupleapp.lovecounter.domain.CounterSettings
import dev.coupleapp.shared.observability.ProductAnalytics
import dev.coupleapp.shared.observability.ProductAnalyticsEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProductAnalyticsTest {
    private class RecordingAnalytics : ProductAnalytics {
        val events = mutableListOf<ProductAnalyticsEvent>()
        override fun track(event: ProductAnalyticsEvent) { events += event }
    }

    private class Repository : CounterRepository {
        val settings = MutableStateFlow<CounterSettings?>(null)
        override fun observeSettings() = settings
        override fun observePendingCount() = settings.map { if (it == null) 0 else 1 }
        override suspend fun readSettings() = settings.value
        override suspend fun save(draft: CounterDraft, today: LocalDate, expectedRevision: Long?): CounterSettings {
            val value = CounterSettings(
                localOwnerId = "owner",
                displayName = draft.displayName,
                partnerName = draft.partnerName,
                startDate = draft.startDate,
                timeZoneId = draft.timeZoneId,
                localRevision = (settings.value?.localRevision ?: 0) + 1,
            )
            settings.value = value
            return value
        }
    }

    @Test
    fun savedAndFailedEventsContainOnlyAllowlistedTechnicalFields() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = Repository()
        val analytics = RecordingAnalytics()
        val session = CounterSession(repository, {}, analytics = analytics)
        try {
            var result: CounterActionResult? = null
            session.save("Private Alice", "Private Bob", "2020-02-03", "Asia/Ho_Chi_Minh", 0) { result = it }
            runCurrent()
            assertNull(result?.errorCode)

            session.save("Changed private name", "Private Bob", "2020-02-03", "Asia/Ho_Chi_Minh", 1) { result = it }
            runCurrent()
            assertNull(result?.errorCode)

            session.save("Secret", "Secret", "not-a-date", "Asia/Ho_Chi_Minh", 2) { result = it }
            runCurrent()
            assertEquals("invalid_date", result?.errorCode)

            assertEquals(listOf("counter_setup_completed", "counter_updated", "counter_save_failed"), analytics.events.map { it.name })
            assertEquals("invalid_date", analytics.events.last().parameters["error_code"])
            analytics.events.forEach { event ->
                assertEquals("1", event.parameters["schema_version"])
                assertTrue(event.parameters.getValue("event_id").matches(Regex("[0-9a-f]{32}")))
                assertTrue(event.parameters.keys.all { it in setOf("event_id", "schema_version", "error_code") })
                val payload = event.name + event.parameters.toString()
                listOf("Alice", "Bob", "Secret", "2020-02-03", "Asia/Ho_Chi_Minh", "owner").forEach {
                    assertFalse(payload.contains(it))
                }
            }
        } finally {
            session.close()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun analyticsFailureNeverChangesSaveResult() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val session = CounterSession(Repository(), {}, analytics = ProductAnalytics { error("collector unavailable") })
        try {
            var result: CounterActionResult? = null
            session.save("Name", "", "2020-01-01", "UTC", 0) { result = it }
            runCurrent()
            assertNull(result?.errorCode)
        } finally {
            session.close()
            Dispatchers.resetMain()
        }
    }
}

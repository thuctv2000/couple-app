package dev.coupleapp.lovecounter.domain

import kotlin.test.*

class CounterRetryPolicyTest {
    @Test fun transientBackoffIsBoundedAndStops() {
        val policy = CounterRetryPolicy { 0.5 }
        assertEquals(listOf(2_000L, 4_000L, 8_000L, 16_000L, 32_000L, 60_000L), (1..6).map { policy.delayMillis(CounterSyncResult.RetryLater, it) })
        assertNull(policy.delayMillis(CounterSyncResult.RetryLater, 7))
        assertEquals(1_000L, CounterRetryPolicy { 0.0 }.delayMillis(CounterSyncResult.RetryLater, 1))
        assertEquals(60_000L, CounterRetryPolicy { 1.0 }.delayMillis(CounterSyncResult.RetryLater, 6))
    }
    @Test fun permanentErrorsAndSuccessDoNotRetry() {
        val policy = CounterRetryPolicy()
        for (result in listOf(CounterSyncResult.Conflict, CounterSyncResult.Forbidden, CounterSyncResult.AccountChanged, CounterSyncResult.AuthRequired, CounterSyncResult.InvalidResponse, CounterSyncResult.InvalidOperation, CounterSyncResult.StorageUnavailable, CounterSyncResult.Drained(1))) {
            assertNull(policy.delayMillis(result, 1))
        }
    }
}

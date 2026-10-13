package dev.coupleapp.shared

import dev.coupleapp.lovecounter.domain.*
import dev.coupleapp.shared.observability.ProductAnalytics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CounterSessionRetryTest {
    private class Fixture {
        val pending = MutableStateFlow(1)
        val analyticsEvents = mutableListOf<String>()
        var calls = 0
        private val op = PreparedCounterOperation("operation", "uid", 0, 1, 1, CounterDraft("Name", "", LocalDate(2020, 1, 1), "UTC"))
        private val repository = object : CounterRepository {
            override fun observeSettings(): Flow<CounterSettings?> = flowOf(null)
            override fun observePendingCount() = pending
            override suspend fun readSettings(): CounterSettings? = null
            override suspend fun save(draft: CounterDraft, today: LocalDate, expectedRevision: Long?): CounterSettings = error("not used")
        }
        private val queue = object : CounterSyncQueue {
            override suspend fun bindFirebaseUid(uid: String) {}
            override suspend fun prepareNext(uid: String) = op.takeIf { pending.value > 0 }
            override suspend fun acknowledge(uid: String, operationId: String, serverVersion: Long): Boolean { pending.value = 0; return true }
        }
        private val identity = object : CounterIdentity {
            override fun currentUid() = "uid"
            override suspend fun credential(forceRefresh: Boolean) = CounterCredential("uid", "test-token")
        }
        private val remote = object : CounterRemote {
            override suspend fun apply(credential: CounterCredential, operation: PreparedCounterOperation): CounterRemoteResult {
                calls++
                return if (calls == 1) CounterRemoteResult.RetryLater else CounterRemoteResult.Accepted(op.operationId, 1, op.payload)
            }
        }
        fun session() = CounterSession(
            repository,
            {},
            SyncCounter(queue, identity, remote),
            analytics = ProductAnalytics { analyticsEvents += it.name },
        )
    }

    @Test fun retriesWhileForegroundWithoutAnotherUserAction() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fixture = Fixture()
        val session = fixture.session()
        try {
            session.requestSync(); runCurrent()
            assertEquals(1, fixture.calls)
            assertEquals(1, fixture.pending.value)
            advanceTimeBy(60_001); runCurrent()
            assertEquals(2, fixture.calls)
            assertEquals(0, fixture.pending.value)
            assertEquals(listOf("sync_confirmed"), fixture.analyticsEvents)
        } finally { session.close(); Dispatchers.resetMain() }
    }

    @Test fun backgroundPausesBackoffAndForegroundResumes() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val fixture = Fixture()
        val session = fixture.session()
        try {
            session.requestSync(); runCurrent()
            session.pauseSync(); runCurrent()
            session.requestSync(); runCurrent()
            advanceTimeBy(120_000); runCurrent()
            assertEquals(1, fixture.calls)
            assertEquals(1, fixture.pending.value)
            session.refresh(); runCurrent()
            assertEquals(2, fixture.calls)
            assertEquals(0, fixture.pending.value)
            assertEquals(listOf("sync_confirmed"), fixture.analyticsEvents)
        } finally { session.close(); Dispatchers.resetMain() }
    }
}

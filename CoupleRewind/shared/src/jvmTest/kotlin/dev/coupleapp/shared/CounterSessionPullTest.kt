package dev.coupleapp.shared

import dev.coupleapp.lovecounter.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CounterSessionPullTest {
    @Test fun foregroundPullsWithEmptyQueueAndRetriesTransientFailure() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val settings = MutableStateFlow<CounterSettings?>(null)
        var reads = 0
        val repository = object : CounterRepository {
            override fun observeSettings() = settings
            override fun observePendingCount() = flowOf(0)
            override suspend fun readSettings() = settings.value
            override suspend fun save(draft: CounterDraft, today: LocalDate, expectedRevision: Long?): CounterSettings = error("unused")
        }
        val identity = object : CounterIdentity {
            override fun currentUid() = "uid"
            override suspend fun credential(forceRefresh: Boolean) = CounterCredential("uid", "token")
        }
        val queue = object : CounterSyncQueue {
            override suspend fun bindFirebaseUid(uid: String) {}
            override suspend fun prepareNext(uid: String): PreparedCounterOperation? = error("empty queue must not push")
            override suspend fun acknowledge(uid: String, operationId: String, serverVersion: Long): Boolean = error("unused")
        }
        val remote = object : CounterRemote {
            override suspend fun apply(credential: CounterCredential, operation: PreparedCounterOperation): CounterRemoteResult = error("must not push")
        }
        val reader = object : CounterSnapshotReader {
            override suspend fun read(credential: CounterCredential): CounterReadResult {
                reads++
                return if (reads == 1) CounterReadResult.RetryLater else CounterReadResult.Found(CounterSnapshot("uid", 4, CounterDraft("Server", "", LocalDate(2020, 1, 1), "UTC")))
            }
        }
        val store = object : CounterSnapshotStore {
            override suspend fun apply(snapshot: CounterSnapshot): CounterSnapshotApplication {
                val p = snapshot.payload
                settings.value = CounterSettings("owner", p.displayName, p.partnerName, p.startDate, p.timeZoneId, 1)
                return CounterSnapshotApplication.Applied
            }
        }
        val session = CounterSession(repository, {}, SyncCounter(queue, identity, remote), pull = PullCounter(identity, queue, reader, store))
        var state: CounterViewState? = null
        val observation = session.observe { state = it }
        try {
            session.refresh(); runCurrent()
            assertEquals(1, reads)
            assertEquals("retry_later", state?.syncState)
            advanceTimeBy(60_001); runCurrent()
            assertEquals(2, reads)
            assertEquals("Server", state?.displayName)
            assertEquals("synced", state?.syncState)
            assertEquals(0, state?.pendingCount)
            session.pauseSync(); session.requestSync(); runCurrent()
            assertEquals(2, reads)
            session.refresh(); runCurrent()
            assertEquals(3, reads)
        } finally { observation.cancel(); session.close(); Dispatchers.resetMain() }
    }
}

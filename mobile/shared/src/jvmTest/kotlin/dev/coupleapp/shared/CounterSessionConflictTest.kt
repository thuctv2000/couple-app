package dev.coupleapp.shared

import dev.coupleapp.lovecounter.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CounterSessionConflictTest {
    private class Fixture {
        val draft = CounterDraft("Local", "Partner", LocalDate(2020, 1, 1), "UTC")
        val pending = MutableStateFlow(1)
        val settings = MutableStateFlow(CounterSettings("owner", draft.displayName, draft.partnerName, draft.startDate, draft.timeZoneId, 1))
        val conflict = MutableStateFlow<CounterConflict?>(null)
        var uid = "uid"
        var reads = 0
        var sends = 0
        var resolved = false
        val repository = object : CounterRepository {
            override fun observeSettings() = settings
            override fun observePendingCount() = pending
            override suspend fun readSettings() = settings.value
            override suspend fun save(draft: CounterDraft, today: LocalDate, expectedRevision: Long?): CounterSettings = error("unused")
        }
        val identity = object : CounterIdentity {
            override fun currentUid() = uid
            override suspend fun credential(forceRefresh: Boolean) = CounterCredential(uid, "test-token")
        }
        val store = object : CounterConflictStore {
            override fun observeConflict() = conflict
            override suspend fun record(snapshot: CounterSnapshot) { conflict.value = CounterConflict(snapshot, settings.value.localRevision) }
            override suspend fun resolve(uid: String, localRevision: Long, serverVersion: Long, keepLocal: Boolean) {
                val saved = requireNotNull(conflict.value)
                require(saved.snapshot.firebaseUid == uid && saved.localRevision == localRevision && settings.value.localRevision == localRevision && saved.snapshot.serverVersion == serverVersion)
                resolved = true
                conflict.value = null
                if (!keepLocal) {
                    settings.value = settings.value.copy(displayName = saved.snapshot.payload.displayName, localRevision = localRevision + 1)
                    pending.value = 0
                }
            }
        }
        val reader = object : CounterSnapshotReader {
            override suspend fun read(credential: CounterCredential): CounterReadResult {
                reads++
                return CounterReadResult.Found(CounterSnapshot(uid, 5, draft.copy(displayName = "Server")))
            }
        }
        val queue = object : CounterSyncQueue {
            override suspend fun bindFirebaseUid(uid: String) {}
            override suspend fun prepareNext(uid: String): PreparedCounterOperation? {
                if (conflict.value != null) throw CounterConflictPending()
                return if (pending.value == 0) null else PreparedCounterOperation("operation", uid, if (resolved) 5 else 0, 1, 1, draft)
            }
            override suspend fun acknowledge(uid: String, operationId: String, serverVersion: Long): Boolean { pending.value = 0; return true }
        }
        val remote = object : CounterRemote {
            override suspend fun apply(credential: CounterCredential, operation: PreparedCounterOperation): CounterRemoteResult {
                sends++
                return if (resolved) CounterRemoteResult.Accepted(operation.operationId, 6, draft) else CounterRemoteResult.Conflict
            }
        }
        fun session() = CounterSession(repository, {}, SyncCounter(queue, identity, remote), store, LoadCounterConflict(identity, reader, store))
    }

    @Test fun conflictLoadsComparisonAndServerChoiceDoesNotSendLocalPayload() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val f = Fixture(); val session = f.session()
        var state: CounterViewState? = null
        val observation = session.observe { state = it }
        try {
            session.requestSync(); runCurrent()
            assertEquals("conflict", state?.syncState)
            assertEquals("Local", state?.displayName)
            assertEquals("Server", state?.conflict?.displayName)
            assertEquals(1, f.pending.value)
            var result: CounterActionResult? = null
            session.resolveConflict(1, 5, false) { result = it }; runCurrent()
            assertNull(result?.errorCode); assertNotNull(result)
            assertEquals("Server", state?.displayName)
            assertNull(state?.conflict)
            assertEquals(0, f.pending.value)
            assertEquals(1, f.sends)
        } finally { observation.cancel(); session.close(); Dispatchers.resetMain() }
    }

    @Test fun keepLocalResumesSendingAfterExplicitChoice() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val f = Fixture(); val session = f.session()
        try {
            session.requestSync(); runCurrent()
            session.resolveConflict(1, 5, true) { assertNull(it.errorCode) }; runCurrent()
            assertEquals(2, f.sends)
            assertEquals(0, f.pending.value)
        } finally { session.close(); Dispatchers.resetMain() }
    }

    @Test fun editAfterPreviewRejectsStaleChoiceAndRefreshEnablesNewChoice() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val f = Fixture(); val session = f.session()
        var state: CounterViewState? = null
        val observation = session.observe { state = it }
        try {
            session.requestSync(); runCurrent()
            f.settings.value = f.settings.value.copy(localRevision = 2, displayName = "New edit"); runCurrent()
            assertFalse(assertNotNull(state?.conflict).isCurrent)
            session.resolveConflict(1, 5, false) { assertEquals("conflict_changed", it.errorCode) }; runCurrent()
            assertEquals("New edit", f.settings.value.displayName)
            assertEquals(1, f.pending.value)
            session.requestSync(); runCurrent()
            assertTrue(assertNotNull(state?.conflict).isCurrent)
            assertEquals(2, state?.conflict?.localRevision)
            assertEquals(1, f.sends)
            assertEquals(2, f.reads)
        } finally { observation.cancel(); session.close(); Dispatchers.resetMain() }
    }

    @Test fun changedAccountCannotApplyCachedChoice() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val f = Fixture(); val session = f.session()
        try {
            session.requestSync(); runCurrent()
            f.uid = "other"
            session.resolveConflict(1, 5, false) { assertEquals("conflict_changed", it.errorCode) }; runCurrent()
            assertFalse(f.resolved)
            assertEquals(1, f.pending.value)
        } finally { session.close(); Dispatchers.resetMain() }
    }
}

package dev.coupleapp.lovecounter.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

class LoadCounterConflictTest {
    private class Fixture {
        var uid = "uid"
        val refreshes = mutableListOf<Boolean>()
        var recorded: CounterSnapshot? = null
        val snapshot = CounterSnapshot("uid", 5, CounterDraft("Server", "", LocalDate(2020, 1, 1), "UTC"))
        val identity = object : CounterIdentity {
            override fun currentUid() = uid
            override suspend fun credential(forceRefresh: Boolean): CounterCredential {
                refreshes += forceRefresh
                return CounterCredential(uid, "token")
            }
        }
        val store = object : CounterConflictStore {
            override fun observeConflict() = flowOf<CounterConflict?>(null)
            override suspend fun record(snapshot: CounterSnapshot) { recorded = snapshot }
            override suspend fun resolve(uid: String, localRevision: Long, serverVersion: Long, keepLocal: Boolean) {}
        }
        fun loader(read: suspend () -> CounterReadResult) = LoadCounterConflict(identity, object : CounterSnapshotReader {
            override suspend fun read(credential: CounterCredential) = read()
        }, store)
    }
    @Test fun refreshesUnauthorizedExactlyOnceAndStoresAuthenticatedSnapshot() = runTest {
        val f = Fixture(); var reads = 0
        val result = f.loader { if (++reads == 1) CounterReadResult.Unauthorized else CounterReadResult.Found(f.snapshot) }.load()
        assertEquals(CounterSyncResult.Conflict, result)
        assertEquals(listOf(false, true), f.refreshes)
        assertEquals(f.snapshot, f.recorded)
    }
    @Test fun repeatedUnauthorizedStopsWithoutSaving() = runTest {
        val f = Fixture(); var reads = 0
        assertEquals(CounterSyncResult.AuthRequired, f.loader { reads++; CounterReadResult.Unauthorized }.load())
        assertEquals(2, reads)
        assertNull(f.recorded)
    }
    @Test fun accountChangedDuringReadCannotStoreSnapshot() = runTest {
        val f = Fixture()
        assertEquals(CounterSyncResult.AccountChanged, f.loader { f.uid = "other"; CounterReadResult.Found(f.snapshot) }.load())
        assertNull(f.recorded)
    }
    @Test fun missingServerCounterDoesNotInventResolution() = runTest {
        val f = Fixture()
        assertEquals(CounterSyncResult.InvalidResponse, f.loader { CounterReadResult.Missing }.load())
        assertNull(f.recorded)
    }
    @Test fun cancellationPropagates() = runTest {
        val f = Fixture()
        assertFailsWith<CancellationException> { f.loader { throw CancellationException() }.load() }
        assertNull(f.recorded)
    }
}

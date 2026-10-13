package dev.coupleapp.lovecounter.data.local

import dev.coupleapp.lovecounter.domain.*
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.*

class PullCounterTest {
    private class Fixture {
        val dir = Files.createTempDirectory("counter-pull").toFile()
        val path = dir.resolve("counter.db").absolutePath
        var db = openCounterDatabase(path)
        var uid: String? = "uid-a"
        var credentialCalls = 0
        val identity = object : CounterIdentity {
            override fun currentUid() = uid
            override suspend fun credential(forceRefresh: Boolean): CounterCredential? {
                credentialCalls++
                return uid?.let { CounterCredential(it, "test-token") }
            }
        }
        val snapshot = CounterSnapshot("uid-a", 5, CounterDraft("Server", "Partner", LocalDate(2020, 1, 1), "UTC"))
        fun pull(read: suspend () -> CounterReadResult = { CounterReadResult.Found(snapshot) }) = PullCounter(identity, LocalCounterSyncQueue(db), object : CounterSnapshotReader {
            override suspend fun read(credential: CounterCredential) = read()
        }, LocalCounterSnapshotStore(db))
        fun close() { db.close(); dir.deleteRecursively() }
    }

    @Test fun restoresIntoEmptyStoreAndReopenPreservesCursorWithoutOutbox() = runBlocking {
        val f = Fixture()
        try {
            assertEquals(CounterSyncResult.Drained(0), f.pull().run())
            val before = assertNotNull(f.db.counterDao().current())
            f.db.close(); f.db = openCounterDatabase(f.path)
            assertEquals(before, f.db.counterDao().current())
            assertEquals(5L, f.db.counterDao().space()?.lastServerVersion)
            assertEquals(before.localRevision, f.db.counterDao().space()?.lastAcknowledgedRevision)
            assertTrue(f.db.counterDao().operations().isEmpty())
            assertEquals(CounterSyncResult.Drained(0), f.pull().run())
            assertEquals(before.localRevision, f.db.counterDao().current()?.localRevision)
            f.db.counterDao().save("Edited", "Partner", "2020-01-01", "UTC")
            assertEquals(5L, f.db.counterDao().prepareNextOperation("uid-a")?.baseServerVersion)
        } finally { f.close() }
    }

    @Test fun newerSnapshotUpdatesCleanStoreButOlderAndSameVersionDifferentPayloadAreRejected() = runBlocking {
        val f = Fixture()
        try {
            f.pull().run()
            val newer = f.snapshot.copy(serverVersion = 6, payload = f.snapshot.payload.copy(displayName = "New server"))
            assertEquals(CounterSyncResult.Drained(0), f.pull { CounterReadResult.Found(newer) }.run())
            assertEquals("New server", f.db.counterDao().current()?.displayName)
            assertEquals(2L, f.db.counterDao().current()?.localRevision)
            assertEquals(CounterSyncResult.InvalidResponse, f.pull().run())
            assertEquals(CounterSyncResult.InvalidResponse, f.pull { CounterReadResult.Found(f.snapshot.copy(serverVersion = 6)) }.run())
            assertEquals("New server", f.db.counterDao().current()?.displayName)
            assertTrue(f.db.counterDao().operations().isEmpty())
        } finally { f.close() }
    }

    @Test fun editDuringNetworkReadRemainsQueuedAtOldBaseVersion() = runBlocking {
        val f = Fixture()
        try {
            f.pull().run()
            val result = f.pull {
                f.db.counterDao().save("Edit while loading", "Partner", "2020-01-01", "UTC")
                CounterReadResult.Found(f.snapshot.copy(serverVersion = 6))
            }.run()
            assertEquals(CounterSyncResult.MorePending(0), result)
            assertEquals("Edit while loading", f.db.counterDao().current()?.displayName)
            assertEquals(5L, f.db.counterDao().space()?.lastServerVersion)
            assertEquals(5L, f.db.counterDao().prepareNextOperation("uid-a")?.baseServerVersion)
        } finally { f.close() }
    }

    @Test fun accountChangeOrMissingServerDoesNotEraseLocalData() = runBlocking {
        val f = Fixture()
        try {
            f.pull().run()
            assertNull(f.pull { CounterReadResult.Missing }.run())
            assertEquals("Server", f.db.counterDao().current()?.displayName)
            assertEquals(CounterSyncResult.AccountChanged, f.pull { f.uid = "other"; CounterReadResult.Found(f.snapshot.copy(serverVersion = 6)) }.run())
            assertEquals(5L, f.db.counterDao().space()?.lastServerVersion)
            assertEquals(CounterSyncResult.AccountChanged, f.pull().run())
        } finally { f.close() }
    }

    @Test fun noIdentityDoesNotCreateAnonymousOrReadNetwork() = runBlocking {
        val f = Fixture()
        try {
            f.uid = null
            assertNull(f.pull { error("must not read") }.run())
            assertEquals(0, f.credentialCalls)
            assertNull(f.db.counterDao().space())
        } finally { f.close() }
    }

    @Test fun unauthorizedRefreshIsBoundedAndStoredConflictIsPreserved() = runBlocking {
        val f = Fixture()
        try {
            assertEquals(CounterSyncResult.AuthRequired, f.pull { CounterReadResult.Unauthorized }.run())
            assertEquals(2, f.credentialCalls)
            f.pull().run()
            f.db.counterDao().save("Local", "Partner", "2020-01-01", "UTC")
            LocalCounterConflictStore(f.db).record(f.snapshot.copy(serverVersion = 6))
            assertEquals(CounterSyncResult.Conflict, f.pull { CounterReadResult.Found(f.snapshot.copy(serverVersion = 7)) }.run())
            assertEquals("Local", f.db.counterDao().current()?.displayName)
            assertEquals(6L, f.db.counterDao().conflict(assertNotNull(f.db.counterDao().space()).localOwnerId)?.serverVersion)
            assertEquals(1, f.db.counterDao().operations().size)
        } finally { f.close() }
    }
}

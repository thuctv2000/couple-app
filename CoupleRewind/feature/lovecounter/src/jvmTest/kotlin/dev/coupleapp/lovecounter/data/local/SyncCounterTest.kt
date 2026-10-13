package dev.coupleapp.lovecounter.data.local

import dev.coupleapp.lovecounter.domain.*
import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class SyncCounterTest {
    private class Identity : CounterIdentity {
        var uid = "user-a"
        var refreshes = 0
        override fun currentUid() = uid
        override suspend fun credential(forceRefresh: Boolean): CounterCredential {
            if (forceRefresh) refreshes++
            return CounterCredential(uid, if (forceRefresh) "fresh-token" else "initial-token")
        }
    }
    private fun remote(block: suspend (CounterCredential, PreparedCounterOperation) -> CounterRemoteResult) = object : CounterRemote {
        override suspend fun apply(credential: CounterCredential, operation: PreparedCounterOperation) = block(credential, operation)
    }
    private suspend fun database(block: suspend (CounterDatabase) -> Unit) {
        val dir = Files.createTempDirectory("counter-worker").toFile()
        val db = openCounterDatabase(dir.resolve("counter.db").absolutePath)
        try {
            db.counterDao().save("First", "", "2020-01-01", "UTC")
            block(db)
        } finally { db.close(); dir.deleteRecursively() }
    }

    @Test fun refreshRetriesExactOperationAndPreservesEditMadeDuringSend() = runBlocking { database { db ->
        val identity = Identity()
        val sent = mutableListOf<PreparedCounterOperation>()
        val worker = SyncCounter(LocalCounterSyncQueue(db), identity, remote { credential, op ->
            sent += op
            if (sent.size == 1) {
                db.counterDao().save("New edit", "", "2020-01-01", "UTC")
                CounterRemoteResult.Unauthorized
            } else {
                assertEquals("fresh-token", credential.idToken)
                CounterRemoteResult.Accepted(op.operationId, op.baseServerVersion + 1, op.payload)
            }
        })
        assertEquals(CounterSyncResult.MorePending(1), worker.run(maxOperations = 1))
        assertEquals(sent[0], sent[1])
        assertEquals(1, identity.refreshes)
        assertEquals("New edit", db.counterDao().current()?.displayName)
        assertEquals(1, db.counterDao().operations().size)
        // A subsequent run drains the remaining revision using its new base version.
        val next = SyncCounter(LocalCounterSyncQueue(db), identity, remote { _, op ->
            assertEquals(1L, op.baseServerVersion)
            CounterRemoteResult.Accepted(op.operationId, 2, op.payload)
        })
        assertEquals(CounterSyncResult.Drained(1), next.run())
    } }

    @Test fun invalidAckAndAccountChangeKeepOperationForSafeRetry() = runBlocking { database { db ->
        val identity = Identity()
        val invalid = SyncCounter(LocalCounterSyncQueue(db), identity, remote { _, op ->
            CounterRemoteResult.Accepted(op.operationId, 1, op.payload.copy(displayName = "Unexpected payload"))
        })
        assertEquals(CounterSyncResult.InvalidResponse, invalid.run())
        val original = db.counterDao().operations().single()
        val switched = SyncCounter(LocalCounterSyncQueue(db), identity, remote { _, op ->
            identity.uid = "user-b"
            CounterRemoteResult.Accepted(op.operationId, 1, op.payload)
        })
        assertEquals(CounterSyncResult.AccountChanged, switched.run())
        assertEquals(listOf(original), db.counterDao().operations())
        assertEquals(CounterSyncResult.AccountChanged, switched.run())
    } }

    @Test fun failuresDoNotDrainAndUnauthorizedRefreshIsBounded() = runBlocking {
        for ((response, expected) in listOf(
            CounterRemoteResult.Conflict to CounterSyncResult.Conflict,
            CounterRemoteResult.Forbidden to CounterSyncResult.Forbidden,
            CounterRemoteResult.InvalidOperation to CounterSyncResult.InvalidOperation,
            CounterRemoteResult.RetryLater to CounterSyncResult.RetryLater,
            CounterRemoteResult.Unauthorized to CounterSyncResult.AuthRequired,
        )) database { db ->
            val identity = Identity()
            var calls = 0
            val worker = SyncCounter(LocalCounterSyncQueue(db), identity, remote { _, _ -> calls++; response })
            assertEquals(expected, worker.run())
            assertEquals(if (response == CounterRemoteResult.Unauthorized) 2 else 1, calls)
            assertEquals(1, db.counterDao().operations().size)
            assertEquals(0L, db.counterDao().space()?.lastServerVersion)
        }
    }

    @Test fun cancellationIsPropagatedAndDoesNotAcknowledge() = runBlocking { database { db ->
        val worker = SyncCounter(LocalCounterSyncQueue(db), Identity(), remote { _, _ -> throw CancellationException("stopped") })
        assertFailsWith<CancellationException> { worker.run() }
        assertEquals(1, db.counterDao().operations().size)
        assertEquals(0L, db.counterDao().space()?.lastServerVersion)
    } }
}

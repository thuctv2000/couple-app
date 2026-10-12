package dev.coupleapp.lovecounter.data.local

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class CounterSyncQueueTest {
    @Test fun ackPreservesLaterEditAndNextOperationUsesNewVersion() = runBlocking {
        val dir = Files.createTempDirectory("counter-sync").toFile()
        val path = dir.resolve("counter.db").absolutePath
        var db = openCounterDatabase(path)
        try {
            var dao = db.counterDao()
            dao.save("First", "", "2020-01-01", "UTC")
            dao.bindFirebaseUid("user-a")
            val sent = assertNotNull(dao.prepareNextOperation("user-a"))
            assertEquals(0L, sent.baseServerVersion)
            dao.save("New edit during send", "", "2020-01-01", "UTC")
            db.close()
            db = openCounterDatabase(path)
            dao = db.counterDao()
            assertEquals(sent, dao.prepareNextOperation("user-a"))
            val prepared = assertNotNull(LocalCounterSyncQueue(db).prepareNext("user-a"))
            assertEquals(sent.operationId, prepared.operationId)
            assertEquals("First", prepared.payload.displayName)
            assertEquals(0L, prepared.baseServerVersion)
            assertTrue(dao.acknowledge("user-a", sent.operationId, 1))
            assertFalse(dao.acknowledge("user-a", sent.operationId, 1))
            assertEquals("New edit during send", dao.current()?.displayName)
            assertEquals(1, dao.operations().size)
            val next = assertNotNull(dao.prepareNextOperation("user-a"))
            assertEquals(1L, next.baseServerVersion)
            assertEquals("New edit during send", next.displayName)
            assertTrue(dao.acknowledge("user-a", next.operationId, 2))
            assertNull(dao.prepareNextOperation("user-a"))
            assertEquals(2L, dao.space()?.lastServerVersion)
            assertEquals(2L, dao.space()?.lastAcknowledgedRevision)
        } finally { db.close(); dir.deleteRecursively() }
    }

    @Test fun accountMismatchAndMalformedAckLeaveQueueUntouched() = runBlocking {
        val dir = Files.createTempDirectory("counter-scope").toFile()
        val db = openCounterDatabase(dir.resolve("counter.db").absolutePath)
        try {
            val dao = db.counterDao()
            dao.save("Local", "", "2020-01-01", "UTC")
            dao.bindFirebaseUid("user-a")
            val sent = assertNotNull(dao.prepareNextOperation("user-a"))
            assertFailsWith<IllegalArgumentException> { dao.bindFirebaseUid("user-b") }
            assertFailsWith<IllegalArgumentException> { dao.prepareNextOperation("user-b") }
            assertFailsWith<IllegalArgumentException> { dao.acknowledge("user-b", sent.operationId, 1) }
            assertFailsWith<IllegalArgumentException> { dao.acknowledge("user-a", sent.operationId, 9) }
            assertEquals(listOf(sent), dao.operations())
            assertEquals("user-a", dao.space()?.firebaseUid)
            assertEquals(0L, dao.space()?.lastServerVersion)
        } finally { db.close(); dir.deleteRecursively() }
    }

    @Test fun migrationFromV1PreservesLocalDataAndUnsentQueue() = runBlocking {
        val dir = Files.createTempDirectory("counter-migration").toFile()
        val path = dir.resolve("counter.db").absolutePath
        val connection = BundledSQLiteDriver().open(path)
        try {
            // Fixture exported from committed Room schema v1, including identity hash.
            val schema = checkNotNull(javaClass.getResource("/counter-v1.sql")).readText()
            schema.lineSequence().filter { it.isNotBlank() }.forEach(connection::execSQL)
            connection.execSQL("INSERT INTO local_spaces VALUES (0, 'local-owner', NULL)")
            connection.execSQL("INSERT INTO counter_settings VALUES ('local-owner', 'Linh', 'Nam', '2020-01-01', 'UTC', 1)")
            connection.execSQL("INSERT INTO counter_outbox VALUES ('op-before-upgrade', 'local-owner', NULL, NULL, 1, 'Linh', 'Nam', '2020-01-01', 'UTC', 1)")
        } finally { connection.close() }
        val db = openCounterDatabase(path)
        try {
            val dao = db.counterDao()
            assertEquals("Linh", dao.current()?.displayName)
            assertEquals("local-owner", dao.space()?.localOwnerId)
            assertEquals(0L, dao.space()?.lastServerVersion)
            assertEquals(0L, dao.space()?.lastAcknowledgedRevision)
            assertEquals("op-before-upgrade", dao.operations().single().operationId)
            dao.bindFirebaseUid("user-a")
            assertEquals("user-a", dao.prepareNextOperation("user-a")?.firebaseUid)
        } finally { db.close(); dir.deleteRecursively() }
    }
}

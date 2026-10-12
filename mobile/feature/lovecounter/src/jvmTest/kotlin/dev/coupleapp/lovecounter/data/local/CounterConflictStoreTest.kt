package dev.coupleapp.lovecounter.data.local

import dev.coupleapp.lovecounter.domain.*
import java.nio.file.Files
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.*
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL

class CounterConflictStoreTest {
    private val server = CounterSnapshot("uid-a", 5, CounterDraft("Server", "", LocalDate(2020, 2, 2), "UTC"))

    @Test fun migrationFromV2PreservesBoundOwnerAndServerCursor() = runBlocking {
        val dir = Files.createTempDirectory("counter-conflict-migration").toFile()
        val path = dir.resolve("counter.db").absolutePath
        val connection = BundledSQLiteDriver().open(path)
        try {
            checkNotNull(javaClass.getResource("/counter-v2.sql")).readText().lineSequence().filter { it.isNotBlank() }.forEach(connection::execSQL)
            connection.execSQL("INSERT INTO local_spaces VALUES (0, 'owner', 'uid-a', 4, 1)")
            connection.execSQL("INSERT INTO counter_settings VALUES ('owner', 'Local', '', '2020-01-01', 'UTC', 2)")
            connection.execSQL("INSERT INTO counter_outbox VALUES ('before-upgrade', 'owner', 'uid-a', 4, 2, 'Local', '', '2020-01-01', 'UTC', 1)")
        } finally { connection.close() }
        val db = openCounterDatabase(path)
        try {
            val dao = db.counterDao()
            assertEquals("uid-a", dao.space()?.firebaseUid)
            assertEquals(4L, dao.space()?.lastServerVersion)
            assertEquals(1L, dao.space()?.lastAcknowledgedRevision)
            assertEquals("before-upgrade", dao.operations().single().operationId)
            LocalCounterConflictStore(db).record(server)
            assertEquals(server, LocalCounterConflictStore(db).observeConflict().first()?.snapshot)
        } finally { db.close(); dir.deleteRecursively() }
    }

    @Test fun reopenKeepsBothVersionsAndKeepLocalCreatesFreshOperation() = runBlocking {
        val dir = Files.createTempDirectory("counter-conflict").toFile()
        val path = dir.resolve("counter.db").absolutePath
        var db = openCounterDatabase(path)
        try {
            db.counterDao().save("Local", "", "2020-01-01", "UTC")
            db.counterDao().bindFirebaseUid("uid-a")
            val old = assertNotNull(db.counterDao().prepareNextOperation("uid-a"))
            LocalCounterConflictStore(db).record(server)
            db.close(); db = openCounterDatabase(path)
            val store = LocalCounterConflictStore(db)
            assertEquals("Local", db.counterDao().current()?.displayName)
            val conflict = assertNotNull(store.observeConflict().first())
            assertEquals(server, conflict.snapshot)
            assertFailsWith<CounterConflictPending> { db.counterDao().prepareNextOperation("uid-a") }
            store.resolve("uid-a", conflict.localRevision, 5, keepLocal = true)
            val fresh = assertNotNull(db.counterDao().prepareNextOperation("uid-a"))
            assertNotEquals(old.operationId, fresh.operationId)
            assertEquals("Local", fresh.displayName)
            assertEquals(5L, fresh.baseServerVersion)
            assertEquals(1, db.counterDao().operations().size)
            assertNull(store.observeConflict().first())
            assertTrue(db.counterDao().acknowledge("uid-a", fresh.operationId, 6))
        } finally { db.close(); dir.deleteRecursively() }
    }

    @Test fun chooseServerClearsOnlyThisQueueAndKeepsServerVersion() = runBlocking {
        val dir = Files.createTempDirectory("counter-conflict").toFile()
        val db = openCounterDatabase(dir.resolve("counter.db").absolutePath)
        try {
            val dao = db.counterDao()
            dao.save("Local", "", "2020-01-01", "UTC")
            dao.bindFirebaseUid("uid-a")
            val store = LocalCounterConflictStore(db)
            store.record(server)
            store.resolve("uid-a", 1, 5, keepLocal = false)
            assertEquals("Server", dao.current()?.displayName)
            assertEquals("2020-02-02", dao.current()?.startDate)
            assertEquals(5L, dao.space()?.lastServerVersion)
            assertTrue(dao.operations().isEmpty())
            assertNull(store.observeConflict().first())
        } finally { db.close(); dir.deleteRecursively() }
    }

    @Test fun staleChoiceOrWrongUidCannotDestroyNewLocalEdit() = runBlocking {
        val dir = Files.createTempDirectory("counter-conflict").toFile()
        val db = openCounterDatabase(dir.resolve("counter.db").absolutePath)
        try {
            val dao = db.counterDao()
            dao.save("Local", "", "2020-01-01", "UTC")
            dao.bindFirebaseUid("uid-a")
            val store = LocalCounterConflictStore(db)
            store.record(server)
            assertFailsWith<IllegalArgumentException> { store.record(server.copy(firebaseUid = "uid-b")) }
            assertFailsWith<IllegalArgumentException> { store.resolve("uid-b", 1, 5, false) }
            dao.save("Edited while comparing", "", "2020-01-01", "UTC")
            assertFailsWith<IllegalArgumentException> { store.resolve("uid-a", 1, 5, false) }
            assertEquals("Edited while comparing", dao.current()?.displayName)
            assertEquals(2, dao.operations().size)
            assertEquals(server, store.observeConflict().first()?.snapshot)
        } finally { db.close(); dir.deleteRecursively() }
    }
}

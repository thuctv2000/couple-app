package dev.coupleapp.lovecounter.data.local

import dev.coupleapp.lovecounter.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import java.nio.file.Files
import kotlin.test.*

class LocalCounterRepositoryTest {
    private val today = LocalDate(2026, 10, 9)
    private val draft = CounterDraft(" Linh ", "Nam", LocalDate(2026, 6, 2), "Asia/Ho_Chi_Minh")

    @Test fun reopenRestoresOwnerDataAndImmutableQueue() = runBlocking {
        val directory = Files.createTempDirectory("couple-test").toFile()
        val path = directory.resolve("couple.db").absolutePath
        var database = openCounterDatabase(path)
        try {
            var repository = LocalCounterRepository(database)
            val first = repository.save(draft, today)
            val second = repository.save(draft.copy(displayName = "Linh mới"), today)
            assertEquals(first.localOwnerId, second.localOwnerId)
            database.close()
            database = openCounterDatabase(path)
            repository = LocalCounterRepository(database)
            assertEquals(second, repository.readSettings())
            assertEquals(second, repository.observeSettings().first())
            assertEquals(2, repository.observePendingCount().first())
            val queue = database.counterDao().operations()
            assertEquals(listOf("Linh", "Linh mới"), queue.map { it.displayName })
            assertEquals(2, queue.map { it.operationId }.toSet().size)
            assertTrue(queue.all { it.firebaseUid == null })
        } finally { database.close(); directory.deleteRecursively() }
    }

    @Test fun invalidInputWritesNothingAndUnchangedSaveIsNoOp() = runBlocking {
        val dir = Files.createTempDirectory("couple-test").toFile()
        val database = openCounterDatabase(dir.resolve("couple.db").absolutePath)
        try {
            val repo = LocalCounterRepository(database)
            assertFailsWith<IllegalArgumentException> { repo.save(draft.copy(startDate = LocalDate(2027, 1, 1)), today) }
            assertFailsWith<IllegalArgumentException> { repo.save(draft.copy(timeZoneId = "invalid-zone"), today) }
            assertNull(repo.readSettings())
            assertEquals(0, repo.observePendingCount().first())
            val first = repo.save(draft, today)
            assertEquals(first, repo.save(draft, today))
            assertEquals(1, repo.observePendingCount().first())
        } finally { database.close(); dir.deleteRecursively() }
    }

    @Test fun concurrentSavesStayInOneSpaceWithUniqueRevisions() = runBlocking {
        val dir = Files.createTempDirectory("couple-test").toFile()
        val database = openCounterDatabase(dir.resolve("couple.db").absolutePath)
        try {
            val repo = LocalCounterRepository(database)
            coroutineScope { (1..12).map { async(Dispatchers.Default) { repo.save(draft.copy(displayName = "Name $it"), today) } }.awaitAll() }
            val operations = database.counterDao().operations()
            assertEquals(12, operations.size)
            assertEquals(1, operations.map { it.localOwnerId }.toSet().size)
            assertEquals((1L..12L).toList(), operations.map { it.localRevision })
        } finally { database.close(); dir.deleteRecursively() }
    }
    @Test fun outboxFailureRollsBackTheCounterUpdate() = runBlocking {
        val dir = Files.createTempDirectory("couple-test").toFile()
        val database = openCounterDatabase(dir.resolve("couple.db").absolutePath)
        try {
            val dao = database.counterDao()
            val original = dao.save("Linh", "Nam", "2026-06-02", "Asia/Ho_Chi_Minh", "same-operation")
            assertFails { dao.save("Must roll back", "Nam", "2026-06-03", "Asia/Ho_Chi_Minh", "same-operation") }
            assertEquals(original, dao.current())
            assertEquals(1, dao.operations().size)
        } finally { database.close(); dir.deleteRecursively() }
    }

    @Test fun staleEditorAfterPullCannotReplaceNewerDataOrCreateOperation() = runBlocking {
        val dir = Files.createTempDirectory("counter-edit-version").toFile()
        val db = openCounterDatabase(dir.resolve("counter.db").absolutePath)
        try {
            val repo = LocalCounterRepository(db)
            db.counterDao().bindFirebaseUid("uid")
            val snapshots = LocalCounterSnapshotStore(db)
            val first = CounterSnapshot("uid", 1, draft.copy(displayName = "Original"))
            snapshots.apply(first)
            val openedRevision = assertNotNull(repo.readSettings()).localRevision
            snapshots.apply(first.copy(serverVersion = 2, payload = draft.copy(displayName = "New from server")))
            assertFailsWith<CounterEditChanged> { repo.save(draft.copy(displayName = "Old editor"), today, openedRevision) }
            assertEquals("New from server", repo.readSettings()?.displayName)
            assertTrue(db.counterDao().operations().isEmpty())
            val latestRevision = assertNotNull(repo.readSettings()).localRevision
            val edited = repo.save(draft.copy(displayName = "Reviewed edit"), today, latestRevision)
            assertEquals(latestRevision + 1, edited.localRevision)
            assertEquals(2L, db.counterDao().prepareNextOperation("uid")?.baseServerVersion)
        } finally { db.close(); dir.deleteRecursively() }
    }

    @Test fun concurrentEditorsAtSameRevisionAllowOnlyOneWrite() = runBlocking {
        val dir = Files.createTempDirectory("counter-edit-race").toFile()
        val db = openCounterDatabase(dir.resolve("counter.db").absolutePath)
        try {
            val repo = LocalCounterRepository(db)
            val outcomes = coroutineScope {
                (1..2).map { id -> async(Dispatchers.Default) {
                    try { repo.save(draft.copy(displayName = "Editor $id"), today, 0); true }
                    catch (_: CounterEditChanged) { false }
                } }.awaitAll()
            }
            assertEquals(1, outcomes.count { it })
            assertEquals(1, db.counterDao().operations().size)
            assertEquals(1L, repo.readSettings()?.localRevision)
        } finally { db.close(); dir.deleteRecursively() }
    }

}

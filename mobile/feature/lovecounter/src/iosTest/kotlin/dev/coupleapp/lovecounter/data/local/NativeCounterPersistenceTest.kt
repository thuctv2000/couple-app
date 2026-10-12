package dev.coupleapp.lovecounter.data.local

import androidx.room.Room
import dev.coupleapp.lovecounter.domain.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import kotlin.test.*
import kotlin.uuid.Uuid

@OptIn(ExperimentalForeignApi::class)
class NativeCounterPersistenceTest {
    @Test fun nativeRoomReopensImmutableOutboxAndAcceptsServerSnapshot() = runTest {
        val directory = NSTemporaryDirectory() + "couple-native-test-" + Uuid.random().toString()
        val files = NSFileManager.defaultManager
        check(files.createDirectoryAtPath(directory, true, null, null))
        val path = "$directory/counter.db"
        fun open() = buildCounterDatabase(Room.databaseBuilder<CounterDatabase>(name = path))
        var database = open()
        try {
            val draft = CounterDraft("iOS local", "Partner", LocalDate(2020, 1, 1), "Asia/Ho_Chi_Minh")
            val saved = LocalCounterRepository(database).save(draft, LocalDate(2026, 1, 1), 0)
            val queue = LocalCounterSyncQueue(database)
            queue.bindFirebaseUid("test-uid")
            val prepared = assertNotNull(queue.prepareNext("test-uid"))
            database.close()
            database = open()
            assertEquals(saved, LocalCounterRepository(database).readSettings())
            val reopenedQueue = LocalCounterSyncQueue(database)
            assertEquals(prepared, reopenedQueue.prepareNext("test-uid"))
            assertEquals(1, LocalCounterRepository(database).observePendingCount().first())
            assertTrue(reopenedQueue.acknowledge("test-uid", prepared.operationId, 1))
            assertEquals(0, LocalCounterRepository(database).observePendingCount().first())
            val server = CounterSnapshot("test-uid", 2, draft.copy(displayName = "iOS received"))
            assertEquals(CounterSnapshotApplication.Applied, LocalCounterSnapshotStore(database).apply(server))
            database.close()
            database = open()
            assertEquals("iOS received", LocalCounterRepository(database).readSettings()?.displayName)
            assertEquals(2L, database.counterDao().space()?.lastServerVersion)
            assertTrue(database.counterDao().operations().isEmpty())
        } finally {
            database.close()
            files.removeItemAtPath(directory, null)
        }
    }
}

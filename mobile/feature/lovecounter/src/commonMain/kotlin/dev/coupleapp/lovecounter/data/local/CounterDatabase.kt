package dev.coupleapp.lovecounter.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid
import dev.coupleapp.lovecounter.domain.CounterEditChanged
import dev.coupleapp.lovecounter.domain.CounterConflictPending
import dev.coupleapp.lovecounter.domain.CounterSnapshotApplication

@Entity(tableName = "counter_conflicts")
data class CounterConflictEntity(
    @PrimaryKey val localOwnerId: String,
    val firebaseUid: String,
    val localRevision: Long,
    val serverVersion: Long,
    val displayName: String,
    val partnerName: String,
    val startDate: String,
    val timeZoneId: String,
)

@Entity(tableName = "local_spaces")
data class LocalSpaceEntity(
    @PrimaryKey val slot: Int = 0,
    val localOwnerId: String,
    val firebaseUid: String? = null,
    @ColumnInfo(defaultValue = "0") val lastServerVersion: Long = 0,
    @ColumnInfo(defaultValue = "0") val lastAcknowledgedRevision: Long = 0,
)

@Entity(tableName = "counter_settings")
data class CounterEntity(
    @PrimaryKey val localOwnerId: String,
    val displayName: String,
    val partnerName: String,
    val startDate: String,
    val timeZoneId: String,
    val localRevision: Long,
)

// Immutable payload: a later edit must not change an operation already sent.
@Entity(tableName = "counter_outbox", indices = [Index("localOwnerId")])
data class CounterOperationEntity(
    @PrimaryKey val operationId: String,
    val localOwnerId: String,
    val firebaseUid: String? = null,
    val baseServerVersion: Long? = null,
    val localRevision: Long,
    val displayName: String,
    val partnerName: String,
    val startDate: String,
    val timeZoneId: String,
    val payloadVersion: Int = 1,
)

@Dao
abstract class CounterDao {
    @Query("SELECT * FROM counter_conflicts WHERE localOwnerId = (SELECT localOwnerId FROM local_spaces WHERE slot = 0)")
    abstract fun observeConflict(): Flow<CounterConflictEntity?>

    @Query("SELECT * FROM counter_conflicts WHERE localOwnerId = :owner")
    abstract suspend fun conflict(owner: String): CounterConflictEntity?

    @Upsert
    abstract suspend fun upsertConflict(value: CounterConflictEntity)

    @Query("DELETE FROM counter_conflicts WHERE localOwnerId = :owner")
    abstract suspend fun deleteConflict(owner: String)

    @Query("DELETE FROM counter_outbox WHERE localOwnerId = :owner")
    abstract suspend fun deletePendingForOwner(owner: String)

    @Transaction
    open suspend fun applySnapshot(uid: String, serverVersion: Long, displayName: String, partnerName: String, startDate: String, timeZoneId: String): CounterSnapshotApplication {
        val owner = requireNotNull(space())
        require(owner.firebaseUid == uid) { "account_conflict" }
        if (serverVersion <= 0 || serverVersion < owner.lastServerVersion) return CounterSnapshotApplication.Stale
        if (conflict(owner.localOwnerId) != null) return CounterSnapshotApplication.Conflict
        if (firstOperation(owner.localOwnerId) != null) return CounterSnapshotApplication.LocalChanges
        val local = current()
        if (local != null && local.localRevision != owner.lastAcknowledgedRevision) return CounterSnapshotApplication.Stale
        if (serverVersion == owner.lastServerVersion) {
            return if (local != null && local.displayName == displayName && local.partnerName == partnerName && local.startDate == startDate && local.timeZoneId == timeZoneId)
                CounterSnapshotApplication.Unchanged else CounterSnapshotApplication.Stale
        }
        val previousRevision = local?.localRevision ?: 0
        check(previousRevision < Long.MAX_VALUE) { "revision_overflow" }
        val revision = previousRevision + 1
        upsertCounter(CounterEntity(owner.localOwnerId, displayName, partnerName, startDate, timeZoneId, revision))
        updateSpace(owner.copy(lastServerVersion = serverVersion, lastAcknowledgedRevision = revision))
        return CounterSnapshotApplication.Applied
    }

    @Transaction
    open suspend fun recordConflict(uid: String, serverVersion: Long, displayName: String, partnerName: String, startDate: String, timeZoneId: String) {
        val owner = requireNotNull(space())
        require(owner.firebaseUid == uid) { "account_conflict" }
        require(serverVersion > 0 && serverVersion >= owner.lastServerVersion) { "invalid_server_version" }
        val local = requireNotNull(current())
        upsertConflict(CounterConflictEntity(owner.localOwnerId, uid, local.localRevision, serverVersion, displayName, partnerName, startDate, timeZoneId))
    }

    @Transaction
    open suspend fun resolveConflict(uid: String, localRevision: Long, serverVersion: Long, keepLocal: Boolean) {
        val owner = requireNotNull(space())
        val saved = requireNotNull(conflict(owner.localOwnerId)) { "conflict_missing" }
        val local = requireNotNull(current())
        require(owner.firebaseUid == uid && saved.firebaseUid == uid) { "account_conflict" }
        require(local.localRevision == localRevision && saved.localRevision == localRevision && saved.serverVersion == serverVersion) { "conflict_changed" }
        require(serverVersion >= owner.lastServerVersion) { "conflict_changed" }
        require(local.localRevision < Long.MAX_VALUE) { "revision_overflow" }
        val revision = local.localRevision + 1
        val chosen = if (keepLocal) local.copy(localRevision = revision) else CounterEntity(owner.localOwnerId, saved.displayName, saved.partnerName, saved.startDate, saved.timeZoneId, revision)
        upsertCounter(chosen)
        deletePendingForOwner(owner.localOwnerId)
        if (keepLocal) {
            insertOperation(CounterOperationEntity(Uuid.random().toString(), owner.localOwnerId, uid, serverVersion, revision, chosen.displayName, chosen.partnerName, chosen.startDate, chosen.timeZoneId))
        }
        updateSpace(owner.copy(lastServerVersion = serverVersion, lastAcknowledgedRevision = if (keepLocal) owner.lastAcknowledgedRevision else revision))
        deleteConflict(owner.localOwnerId)
    }
    @Query("SELECT * FROM local_spaces WHERE slot = 0")
    abstract suspend fun space(): LocalSpaceEntity?

    @Insert
    abstract suspend fun insertSpace(value: LocalSpaceEntity)

    @Update
    abstract suspend fun updateSpace(value: LocalSpaceEntity)

    @Query("UPDATE counter_outbox SET firebaseUid = :uid WHERE localOwnerId = :owner AND firebaseUid IS NULL")
    abstract suspend fun bindUnsentOperations(owner: String, uid: String)

    @Query("SELECT * FROM counter_outbox WHERE localOwnerId = :owner ORDER BY localRevision LIMIT 1")
    abstract suspend fun firstOperation(owner: String): CounterOperationEntity?

    @Query("SELECT * FROM counter_outbox WHERE operationId = :id")
    abstract suspend fun operation(id: String): CounterOperationEntity?

    @Update
    abstract suspend fun updateOperation(value: CounterOperationEntity)

    @Query("DELETE FROM counter_outbox WHERE operationId = :id")
    abstract suspend fun deleteOperation(id: String)

    @Transaction
    open suspend fun bindFirebaseUid(uid: String) {
        require(uid.isNotBlank() && uid.length <= 128 && '/' !in uid) { "invalid_uid" }
        val owner = space() ?: LocalSpaceEntity(localOwnerId = Uuid.random().toString()).also { insertSpace(it) }
        require(owner.firebaseUid == null || owner.firebaseUid == uid) { "account_conflict" }
        updateSpace(owner.copy(firebaseUid = uid))
        bindUnsentOperations(owner.localOwnerId, uid)
    }

    /** Persist the base before first send. Retries reuse this exact operation. */
    @Transaction
    open suspend fun prepareNextOperation(uid: String): CounterOperationEntity? {
        val owner = space() ?: return null
        require(owner.firebaseUid == uid) { "account_conflict" }
        if (conflict(owner.localOwnerId) != null) throw CounterConflictPending()
        val next = firstOperation(owner.localOwnerId) ?: return null
        require(next.firebaseUid == uid) { "account_conflict" }
        if (next.baseServerVersion != null) return next
        return next.copy(baseServerVersion = owner.lastServerVersion).also { updateOperation(it) }
    }

    /** Atomically advance the server cursor and remove only the matching operation. */
    @Transaction
    open suspend fun acknowledge(uid: String, operationId: String, serverVersion: Long): Boolean {
        val owner = space() ?: return false
        require(owner.firebaseUid == uid) { "account_conflict" }
        val sent = operation(operationId) ?: return false
        require(sent.localOwnerId == owner.localOwnerId && sent.firebaseUid == uid) { "account_conflict" }
        val base = requireNotNull(sent.baseServerVersion) { "operation_not_prepared" }
        require(base < Long.MAX_VALUE && serverVersion == base + 1 && serverVersion > owner.lastServerVersion) { "invalid_ack" }
        require(firstOperation(owner.localOwnerId)?.operationId == operationId) { "out_of_order_ack" }
        updateSpace(owner.copy(lastServerVersion = serverVersion, lastAcknowledgedRevision = sent.localRevision))
        deleteOperation(operationId)
        return true
    }

    @Query("SELECT * FROM counter_settings WHERE localOwnerId = (SELECT localOwnerId FROM local_spaces WHERE slot = 0)")
    abstract suspend fun current(): CounterEntity?

    @Query("SELECT * FROM counter_settings WHERE localOwnerId = (SELECT localOwnerId FROM local_spaces WHERE slot = 0)")
    abstract fun observeCurrent(): Flow<CounterEntity?>

    @Query("SELECT COUNT(*) FROM counter_outbox WHERE localOwnerId = (SELECT localOwnerId FROM local_spaces WHERE slot = 0)")
    abstract fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM counter_outbox ORDER BY localRevision")
    abstract suspend fun operations(): List<CounterOperationEntity>

    @Upsert
    abstract suspend fun upsertCounter(value: CounterEntity)

    @Insert
    abstract suspend fun insertOperation(value: CounterOperationEntity)

    @Transaction
    open suspend fun save(displayName: String, partnerName: String, startDate: String, timeZoneId: String, operationId: String = Uuid.random().toString(), expectedRevision: Long? = null): CounterEntity {
        val owner = space() ?: LocalSpaceEntity(localOwnerId = Uuid.random().toString()).also { insertSpace(it) }
        val previous = current()
        if (expectedRevision != null && expectedRevision != (previous?.localRevision ?: 0)) throw CounterEditChanged()
        check((previous?.localRevision ?: 0) < Long.MAX_VALUE) { "revision_overflow" }
        if (previous != null && previous.displayName == displayName && previous.partnerName == partnerName &&
            previous.startDate == startDate && previous.timeZoneId == timeZoneId) return previous
        val next = CounterEntity(owner.localOwnerId, displayName, partnerName, startDate, timeZoneId, (previous?.localRevision ?: 0) + 1)
        upsertCounter(next)
        insertOperation(CounterOperationEntity(
            operationId = operationId, localOwnerId = owner.localOwnerId,
            firebaseUid = owner.firebaseUid, localRevision = next.localRevision,
            displayName = displayName, partnerName = partnerName, startDate = startDate, timeZoneId = timeZoneId,
        ))
        return next
    }
}

@Database(entities = [LocalSpaceEntity::class, CounterEntity::class, CounterOperationEntity::class, CounterConflictEntity::class], version = 3, exportSchema = true)
@ConstructedBy(CounterDatabaseConstructor::class)
abstract class CounterDatabase : RoomDatabase() {
    abstract fun counterDao(): CounterDao
}

@Suppress("KotlinNoActualForExpect")
expect object CounterDatabaseConstructor : RoomDatabaseConstructor<CounterDatabase> {
    override fun initialize(): CounterDatabase
}

fun buildCounterDatabase(builder: RoomDatabase.Builder<CounterDatabase>): CounterDatabase = builder
    .addMigrations(CounterMigration1To2)
    .addMigrations(CounterMigration2To3)
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()

object CounterMigration1To2 : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE local_spaces ADD COLUMN lastServerVersion INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE local_spaces ADD COLUMN lastAcknowledgedRevision INTEGER NOT NULL DEFAULT 0")
    }
}

object CounterMigration2To3 : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS counter_conflicts (localOwnerId TEXT NOT NULL, firebaseUid TEXT NOT NULL, localRevision INTEGER NOT NULL, serverVersion INTEGER NOT NULL, displayName TEXT NOT NULL, partnerName TEXT NOT NULL, startDate TEXT NOT NULL, timeZoneId TEXT NOT NULL, PRIMARY KEY(localOwnerId))")
    }
}

package dev.coupleapp.lovecounter.domain

data class PreparedCounterOperation(
    val operationId: String,
    val firebaseUid: String,
    val baseServerVersion: Long,
    val localRevision: Long,
    val payloadVersion: Int,
    val payload: CounterDraft,
)

/** Durable queue port; implementations must persist preparation and ACK atomically. */
interface CounterSyncQueue {
    suspend fun bindFirebaseUid(uid: String)
    suspend fun prepareNext(uid: String): PreparedCounterOperation?
    suspend fun acknowledge(uid: String, operationId: String, serverVersion: Long): Boolean
}

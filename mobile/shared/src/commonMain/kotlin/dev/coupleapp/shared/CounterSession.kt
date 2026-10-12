package dev.coupleapp.shared

import dev.coupleapp.lovecounter.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import kotlin.time.Clock

class CounterViewState(
    val loaded: Boolean = false,
    val hasCounter: Boolean = false,
    val localRevision: Long = 0,
    val displayName: String = "",
    val partnerName: String = "",
    val startDateIso: String = "",
    val timeZoneId: String = TimeZone.currentSystemDefault().id,
    val days: Int = 0,
    val pendingCount: Int = 0,
    val saving: Boolean = false,
    val errorCode: String? = null,
    val syncState: String = "not_configured",
    val conflict: CounterConflictView? = null,
)
class CounterConflictView(
    val localRevision: Long,
    val serverVersion: Long,
    val displayName: String,
    val partnerName: String,
    val startDateIso: String,
    val timeZoneId: String,
    val isCurrent: Boolean,
)
class CounterActionResult(val errorCode: String?)
class CounterObservation internal constructor(private val job: Job) { fun cancel() = job.cancel() }

/** Host-owned session. Local reads do not wait for cloud identity or connectivity. */
class CounterSession internal constructor(
    private val repository: CounterRepository,
    private val closeDatabase: () -> Unit,
    private val sync: SyncCounter? = null,
    private val conflicts: CounterConflictStore? = null,
    private val conflictLoader: LoadCounterConflict? = null,
    private val pull: PullCounter? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val saving = MutableStateFlow(false)
    private val syncState = MutableStateFlow(if (sync == null) "not_configured" else "ready")
    private var syncJob: Job? = null
    private var syncPaused = false
    private val retryPolicy = CounterRetryPolicy()
    private val counter = CountLoveDaysAtInstant()
    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun refresh() { syncPaused = false; refreshRequests.tryEmit(Unit); requestSync() }

    fun pauseSync() {
        syncPaused = true
        syncJob?.cancel()
        syncJob = null
        if (syncState.value == "syncing") syncState.value = "pending"
    }

    fun requestSync() {
        if (syncPaused || saving.value) return
        val worker = sync ?: return
        if (syncJob?.isActive == true) {
            if (syncState.value != "retry_later") return
            syncJob?.cancel() // Manual retry/foreground wakes a sleeping backoff.
        }
        syncJob = scope.launch {
            try {
                var failures = 0
                while (true) {
                    syncState.value = "syncing"
                    val hasPending = repository.observePendingCount().first() > 0
                    val sent = if (hasPending) worker.run() else pull?.run()
                    val checked = if (hasPending && sent is CounterSyncResult.Drained && pull != null) pull.run() else sent
                    if (checked == null) { syncState.value = "ready"; return@launch }
                    val result = if (checked == CounterSyncResult.Conflict) conflictLoader?.load() ?: checked else checked
                    syncState.value = when (result) {
                    is CounterSyncResult.Drained -> "synced"
                    is CounterSyncResult.MorePending -> "pending"
                    CounterSyncResult.AuthRequired -> "auth_required"
                    CounterSyncResult.AccountChanged -> "account_conflict"
                    CounterSyncResult.Conflict -> "conflict"
                    CounterSyncResult.Forbidden -> "forbidden"
                    CounterSyncResult.InvalidOperation, CounterSyncResult.InvalidResponse -> "sync_error"
                    CounterSyncResult.RetryLater -> "retry_later"
                    CounterSyncResult.StorageUnavailable -> "storage_unavailable"
                    }
                    when (result) {
                        is CounterSyncResult.MorePending -> { failures = 0; yield() }
                        is CounterSyncResult.Drained -> return@launch
                        else -> {
                            failures++
                            val wait = retryPolicy.delayMillis(result, failures) ?: return@launch
                            delay(wait)
                        }
                    }
                }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { syncState.value = "retry_later" }
        }
    }

    fun observe(onChange: (CounterViewState) -> Unit): CounterObservation {
        val job = scope.launch {
            val ticks = merge(flow { while (true) { emit(Clock.System.now()); delay(15_000) } }, refreshRequests.map { Clock.System.now() })
            val flags = combine(saving, syncState) { busy, status -> busy to status }
            combine(repository.observeSettings(), repository.observePendingCount(), ticks, flags, conflicts?.observeConflict() ?: flowOf(null)) { settings, pending, now, flagsValue, conflict ->
                val result = settings?.let { counter(it.startDate, now, TimeZone.of(it.timeZoneId)) }
                CounterViewState(
                    loaded = true, hasCounter = settings != null, localRevision = settings?.localRevision ?: 0,
                    displayName = settings?.displayName.orEmpty(), partnerName = settings?.partnerName.orEmpty(),
                    startDateIso = settings?.startDate?.toString().orEmpty(),
                    timeZoneId = settings?.timeZoneId ?: TimeZone.currentSystemDefault().id,
                    days = (result as? LoveDays.Count)?.days ?: 0, pendingCount = pending, saving = flagsValue.first,
                    errorCode = if (result == LoveDays.FutureStartDate) "future_date" else null,
                    syncState = flagsValue.second,
                    conflict = conflict?.let {
                        CounterConflictView(it.localRevision, it.snapshot.serverVersion,
                            it.snapshot.payload.displayName, it.snapshot.payload.partnerName,
                            it.snapshot.payload.startDate.toString(), it.snapshot.payload.timeZoneId,
                            settings?.localRevision == it.localRevision)
                    },
                )
            }.catch { emit(CounterViewState(loaded = true, errorCode = "storage_unavailable")) }
                .collect { onChange(it) }
        }
        return CounterObservation(job)
    }

    fun save(displayName: String, partnerName: String, startDateIso: String, timeZoneId: String, expectedRevision: Long, onComplete: (CounterActionResult) -> Unit) {
        scope.launch {
            if (saving.value) { onComplete(CounterActionResult("save_in_progress")); return@launch }
            saving.value = true
            val result = try {
                val date = try { LocalDate.parse(startDateIso) } catch (_: IllegalArgumentException) {
                    onComplete(CounterActionResult("invalid_date")); return@launch
                }
                val zone = try { TimeZone.of(timeZoneId) } catch (_: IllegalArgumentException) {
                    onComplete(CounterActionResult("invalid_time_zone")); return@launch
                }
                repository.save(CounterDraft(displayName, partnerName, date, zone.id), Clock.System.now().toLocalDateTime(zone).date, expectedRevision)
                CounterActionResult(null)
            } catch (error: CancellationException) { throw error
            } catch (_: CounterEditChanged) { CounterActionResult("edit_changed")
            } catch (error: IllegalArgumentException) {
                CounterActionResult(if (error.message == "future_date") "future_date" else "invalid_input")
            } catch (_: Exception) { CounterActionResult("save_failed")
            } finally { saving.value = false }
            onComplete(result)
            if (result.errorCode == null) requestSync()
        }
    }

    fun resolveConflict(localRevision: Long, serverVersion: Long, keepLocal: Boolean, onComplete: (CounterActionResult) -> Unit) {
        scope.launch {
            if (saving.value) { onComplete(CounterActionResult("save_in_progress")); return@launch }
            val loader = conflictLoader ?: run { onComplete(CounterActionResult("not_configured")); return@launch }
            saving.value = true
            val result = try {
                syncJob?.cancelAndJoin()
                syncJob = null
                loader.resolve(localRevision, serverVersion, keepLocal)
                syncState.value = if (keepLocal) "pending" else "ready"
                CounterActionResult(null)
            } catch (e: CancellationException) { throw e
            } catch (_: IllegalArgumentException) { CounterActionResult("conflict_changed")
            } catch (_: Exception) { CounterActionResult("save_failed")
            } finally { saving.value = false }
            onComplete(result)
            if (result.errorCode == null) requestSync()
        }
    }

    fun close() { scope.cancel(); closeDatabase() }
}

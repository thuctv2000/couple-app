package dev.coupleapp.shared

import dev.coupleapp.lovecounter.domain.*
import dev.coupleapp.shared.observability.DisabledProductAnalytics
import dev.coupleapp.shared.observability.ProductAnalytics
import dev.coupleapp.shared.observability.ProductAnalyticsEvent
import dev.coupleapp.shared.observability.ProductEvents
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
    private val analytics: ProductAnalytics = DisabledProductAnalytics,
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
                        is CounterSyncResult.Drained -> {
                            val acknowledged = (sent as? CounterSyncResult.Drained)?.acknowledged ?: 0
                            if (hasPending && acknowledged > 0) track(ProductEvents.syncConfirmed())
                            return@launch
                        }
                        else -> {
                            failures++
                            val wait = retryPolicy.delayMillis(result, failures)
                            if (wait == null) {
                                track(ProductEvents.syncFailed(result.analyticsCode()))
                                return@launch
                            }
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
            if (saving.value) {
                val result = CounterActionResult("save_in_progress")
                track(ProductEvents.counterSaveFailed(requireNotNull(result.errorCode)))
                onComplete(result)
                return@launch
            }
            saving.value = true
            var initial = false
            val result = try {
                val date = try { LocalDate.parse(startDateIso) } catch (_: IllegalArgumentException) { throw CounterInputError("invalid_date") }
                val zone = try { TimeZone.of(timeZoneId) } catch (_: IllegalArgumentException) { throw CounterInputError("invalid_time_zone") }
                initial = repository.readSettings() == null
                repository.save(CounterDraft(displayName, partnerName, date, zone.id), Clock.System.now().toLocalDateTime(zone).date, expectedRevision)
                CounterActionResult(null)
            } catch (error: CancellationException) { throw error
            } catch (error: CounterInputError) { CounterActionResult(error.code)
            } catch (_: CounterEditChanged) { CounterActionResult("edit_changed")
            } catch (error: IllegalArgumentException) {
                CounterActionResult(if (error.message == "future_date") "future_date" else "invalid_input")
            } catch (_: Exception) { CounterActionResult("save_failed")
            } finally { saving.value = false }
            if (result.errorCode == null) track(ProductEvents.counterSaved(initial))
            else track(ProductEvents.counterSaveFailed(result.errorCode))
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
                track(ProductEvents.conflictResolved(keepLocal))
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

    private fun track(event: ProductAnalyticsEvent) {
        try { analytics.track(event) } catch (_: Exception) { /* Analytics must never break the product action. */ }
    }
}

private class CounterInputError(val code: String) : IllegalArgumentException(code)

private fun CounterSyncResult.analyticsCode(): String = when (this) {
    CounterSyncResult.AuthRequired -> "auth_required"
    CounterSyncResult.AccountChanged -> "account_conflict"
    CounterSyncResult.Conflict -> "conflict"
    CounterSyncResult.Forbidden -> "forbidden"
    CounterSyncResult.InvalidOperation -> "invalid_operation"
    CounterSyncResult.InvalidResponse -> "invalid_response"
    CounterSyncResult.RetryLater -> "retry_later"
    CounterSyncResult.StorageUnavailable -> "storage_unavailable"
    is CounterSyncResult.Drained, is CounterSyncResult.MorePending -> "sync_error"
}

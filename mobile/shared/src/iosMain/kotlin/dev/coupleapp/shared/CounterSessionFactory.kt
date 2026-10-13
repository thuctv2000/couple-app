package dev.coupleapp.shared

import dev.coupleapp.lovecounter.data.local.openCounterStore
import dev.coupleapp.lovecounter.data.remote.CounterRemoteConnection
import dev.coupleapp.lovecounter.domain.SyncCounter
import dev.coupleapp.lovecounter.domain.LoadCounterConflict
import dev.coupleapp.lovecounter.domain.PullCounter
import dev.coupleapp.shared.observability.DisabledProductAnalytics
import dev.coupleapp.shared.observability.ProductAnalytics

fun createCounterSession(): CounterSession {
    return createCounterSession(DisabledProductAnalytics)
}

fun createCounterSession(analytics: ProductAnalytics): CounterSession {
    val store = openCounterStore()
    return CounterSession(store.repository, store::close, analytics = analytics)
}

fun createSyncedCounterSession(
    identity: NativeCounterIdentity,
    apiBaseUrl: String,
    allowLocalHttp: Boolean,
    analytics: ProductAnalytics,
): CounterSession {
    val store = openCounterStore()
    val connection = CounterRemoteConnection(apiBaseUrl, allowLocalHttp)
    val adapter = NativeIdentityAdapter(identity)
    return CounterSession(store.repository, { connection.close(); store.close() }, SyncCounter(store.syncQueue, adapter, connection.remote), store.conflicts, LoadCounterConflict(adapter, connection.reader, store.conflicts), PullCounter(adapter, store.syncQueue, connection.reader, store.snapshots), analytics)
}

package dev.coupleapp.shared

import dev.coupleapp.lovecounter.data.local.openCounterStore
import dev.coupleapp.lovecounter.data.auth.firebaseCounterIdentity
import dev.coupleapp.lovecounter.data.remote.CounterRemoteConnection
import dev.coupleapp.lovecounter.domain.SyncCounter
import dev.coupleapp.lovecounter.domain.LoadCounterConflict
import dev.coupleapp.lovecounter.domain.PullCounter

fun createCounterSession(context: android.content.Context, emulator: Boolean = false): CounterSession {
    val store = openCounterStore(context)
    if (emulator) {
        // Emulator-only placeholder; it is not a credential for any cloud project.
        val identity = firebaseCounterIdentity(context, "demo-couple-app", "1:1234567890:android:1234567890abcdef", "AIza" + "0".repeat(35), emulator = true)
        val connection = CounterRemoteConnection("http://10.0.2.2:18080", allowLocalHttp = true)
        return CounterSession(store.repository, { connection.close(); store.close() }, SyncCounter(store.syncQueue, identity, connection.remote), store.conflicts, LoadCounterConflict(identity, connection.reader, store.conflicts), PullCounter(identity, store.syncQueue, connection.reader, store.snapshots))
    }
    return CounterSession(store.repository, store::close)
}

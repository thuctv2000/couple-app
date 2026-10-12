package dev.coupleapp.android.navigation

import androidx.compose.runtime.*
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.coupleapp.android.features.lovecounter.*
import dev.coupleapp.shared.CounterSession
import dev.coupleapp.shared.CounterViewState
import kotlinx.serialization.Serializable

@Serializable data object Start : NavKey
@Serializable data object ChooseDate : NavKey
@Serializable data object Home : NavKey
@Serializable data object Edit : NavKey
@Serializable data object Account : NavKey
@Serializable data object Sync : NavKey

@Composable
fun AppNavigation(session: CounterSession) {
    var state by remember { mutableStateOf(CounterViewState()) }
    var observationAttempt by remember { mutableIntStateOf(0) }
    DisposableEffect(session, observationAttempt) {
        val subscription = session.observe { state = it }
        onDispose { subscription.cancel() }
    }
    val backStack = rememberNavBackStack(Start)
    LaunchedEffect(state.loaded, state.hasCounter) {
        if (state.loaded && state.hasCounter && backStack.lastOrNull() == Start) {
            backStack.clear(); backStack.add(Home)
        }
    }
    fun back() { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
    fun home() { backStack.clear(); backStack.add(Home) }
    if (!state.loaded || state.errorCode == "storage_unavailable") {
        StorageStatus(state.errorCode) { state = CounterViewState(); observationAttempt++ }
        return
    }
    NavDisplay(
        backStack = backStack,
        onBack = { back() },
        entryProvider = entryProvider {
            entry<Start> { WelcomeScreen({ backStack.add(ChooseDate) }, { backStack.add(Account) }) }
            entry<ChooseDate> { EditCounterScreen(state, session, true, { back() }, { home() }) }
            entry<Home> { CounterHomeScreen(state, { backStack.add(Edit) }, { backStack.add(Account) }, { backStack.add(Sync) }) }
            entry<Edit> { EditCounterScreen(state, session, false, { back() }, { home() }) }
            entry<Account> { AccountScreen(state, { back() }, { backStack.add(Sync) }) }
            entry<Sync> { SyncScreen(state, { back() }, session::requestSync, session) }
        },
    )
}

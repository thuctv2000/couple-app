package dev.coupleapp.android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.coupleapp.android.features.lovecounter.LoveCounterScreen
import kotlinx.serialization.Serializable

@Serializable
data object Home : NavKey

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(Home)
    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
        entryProvider = entryProvider { entry<Home> { LoveCounterScreen() } },
    )
}

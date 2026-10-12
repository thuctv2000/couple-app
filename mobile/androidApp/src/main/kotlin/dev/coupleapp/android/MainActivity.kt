package dev.coupleapp.android
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import dev.coupleapp.android.navigation.AppNavigation
class MainActivity : ComponentActivity() {
    override fun onStop() {
        (application as CoupleApplication).counterSession.pauseSync()
        super.onStop()
    }
    override fun onResume() {
        super.onResume()
        (application as CoupleApplication).counterSession.refresh()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val colors = if (isSystemInDarkTheme()) darkColorScheme(
                primary = Color(0xFFE4A6AB), primaryContainer = Color(0xFF59383B), onPrimaryContainer = Color(0xFFFFDADC),
            ) else lightColorScheme(
                primary = Color(0xFFAD5F63), background = Color(0xFFFAF7F2), surface = Color(0xFFFAF7F2),
                onBackground = Color(0xFF302925), onSurface = Color(0xFF302925),
                primaryContainer = Color(0xFFF3DEDA), onPrimaryContainer = Color(0xFF542F33),
            )
            MaterialTheme(colorScheme = colors) { AppNavigation((application as CoupleApplication).counterSession) }
        }
    }
}

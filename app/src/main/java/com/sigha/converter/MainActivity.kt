package com.sigha.converter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sigha.converter.data.IncomingFilesBus
import com.sigha.converter.data.Repo
import com.sigha.converter.data.ThemeMode
import com.sigha.converter.data.extractIncomingUris
import com.sigha.converter.data.takePersistableReadPermission
import com.sigha.converter.ui.SighaNavHost
import com.sigha.converter.ui.WorkbenchViewModel
import com.sigha.converter.ui.theme.SighaTheme

/** Navigation-bar scrims from the Material 3 edge-to-edge guidance. */
private val LightNavigationScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val DarkNavigationScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

/**
 * Single activity host. Shared and viewed files are pushed onto [IncomingFilesBus] instead of
 * being routed directly, so a cold start (before Compose subscribes) and a warm onNewIntent
 * behave the same. The workbench ViewModel is activity-scoped: switching tabs keeps the
 * selection, and conversions keep running in the app-scoped queue.
 */
class MainActivity : ComponentActivity() {

    private val workbenchViewModel: WorkbenchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        routeIncoming(intent)
        setContent {
            val settings by Repo.settings.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // The system-bar appearance follows the *resolved* theme, not the system one.
            // uiMode is in android:configChanges, so a theme flip does not recreate this
            // activity; re-applying here is what keeps bar icons readable on every flip.
            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        LightNavigationScrim,
                        DarkNavigationScrim
                    ) { darkTheme }
                )
            }
            SighaTheme(darkTheme = darkTheme) {
                SighaNavHost(workbenchViewModel = workbenchViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeIncoming(intent)
    }

    /**
     * Android 13+ needs a runtime grant before the conversion progress notification can appear.
     * The conversion itself does not depend on it; a refusal only hides the notification.
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }

    private fun routeIncoming(intent: Intent?) {
        val uris = extractIncomingUris(intent)
        if (uris.isEmpty()) return
        uris.forEach { uri -> takePersistableReadPermission(this, uri) }
        IncomingFilesBus.offer(uris)
    }

    private companion object {
        const val REQUEST_NOTIFICATIONS = 1001
    }
}

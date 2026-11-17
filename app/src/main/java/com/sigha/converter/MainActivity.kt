package com.sigha.converter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sigha.converter.data.IncomingFilesBus
import com.sigha.converter.data.Repo
import com.sigha.converter.data.ThemeMode
import com.sigha.converter.data.extractIncomingUris
import com.sigha.converter.data.takePersistableReadPermission
import com.sigha.converter.ui.SighaNavHost
import com.sigha.converter.ui.WorkbenchViewModel
import com.sigha.converter.ui.theme.SighaTheme

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
        enableEdgeToEdge()
        routeIncoming(intent)
        setContent {
            val settings by Repo.settings.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
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

    private fun routeIncoming(intent: Intent?) {
        val uris = extractIncomingUris(intent)
        if (uris.isEmpty()) return
        uris.forEach { uri -> takePersistableReadPermission(this, uri) }
        IncomingFilesBus.offer(uris)
    }
}

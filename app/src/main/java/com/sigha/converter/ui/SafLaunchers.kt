package com.sigha.converter.ui

import android.content.ActivityNotFoundException
import androidx.activity.result.ActivityResultLauncher

/**
 * Starts a Storage Access Framework picker without letting a device that has no documents
 * provider take the whole app down: `startActivityForResult` throws
 * [ActivityNotFoundException] when nothing can handle ACTION_OPEN_DOCUMENT(/_TREE).
 * Minimal/headless Android images and some vendor builds can be in that state.
 */
fun <I> ActivityResultLauncher<I>.launchSaf(input: I, onMissing: () -> Unit) {
    try {
        launch(input)
    } catch (_: ActivityNotFoundException) {
        onMissing()
    } catch (_: SecurityException) {
        onMissing()
    }
}

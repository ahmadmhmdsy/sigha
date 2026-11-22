package com.sigha.converter.ui

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.sigha.converter.MainActivity

/** Opens a converted file in whatever app can handle it. Returns false when nothing can. */
fun openFileWith(context: Context, uri: Uri, mime: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime.ifBlank { "*/*" })
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return launchSafely(context, intent)
}

/** System share sheet for a converted file. */
fun shareFile(context: Context, uri: Uri, mime: String): Boolean {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mime.ifBlank { "*/*" }
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return launchSafely(context, shareChooser(context, send))
}

/**
 * System share sheet for a whole batch. Files with different targets share as the wildcard
 * type so the chooser still offers every app; a uniform batch keeps its specific MIME type.
 */
fun shareFiles(context: Context, uris: List<Uri>, mime: String): Boolean {
    if (uris.isEmpty()) return false
    val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = mime.ifBlank { "*/*" }
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return launchSafely(context, shareChooser(context, send))
}

/**
 * The chooser hides Sigha's own "share into Sigha" entry: converted files must never loop back
 * into the app and be added again as inputs. Incoming shares from other apps still work.
 */
private fun shareChooser(context: Context, send: Intent): Intent =
    Intent.createChooser(send, null).apply {
        // The platform reads this extra as a ComponentName[] array.
        putExtra(
            Intent.EXTRA_EXCLUDE_COMPONENTS,
            arrayOf(ComponentName(context, MainActivity::class.java)),
        )
    }

/** Opens this app's system settings page (permissions, notifications, storage). */
fun openAppSettings(context: Context): Boolean {
    val intent = Intent(
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return launchSafely(context, intent)
}

/** Opens a web page (used only for model pack license links). */
fun openUrl(context: Context, url: String): Boolean {
    if (url.isBlank()) return false
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return launchSafely(context, intent)
}

private fun launchSafely(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: Exception) {
    false
}

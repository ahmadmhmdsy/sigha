package com.sigha.converter.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

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
    return launchSafely(context, Intent.createChooser(send, null))
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

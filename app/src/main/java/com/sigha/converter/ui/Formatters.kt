package com.sigha.converter.ui

import android.content.Context
import android.text.format.Formatter
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** "2.4 MB" style, using the system's file size formatter. */
fun formatFileSize(context: Context, bytes: Long?): String? {
    val value = bytes ?: return null
    if (value <= 0L) return null
    return Formatter.formatShortFileSize(context, value)
}

/** Date and time in the user's locale and format. */
fun formatDateTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.SHORT,
        Locale.getDefault()
    ).format(Date(timestamp))
}

/** Short date only, for dense list rows. */
fun formatDate(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(timestamp))
}

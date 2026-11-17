package com.sigha.converter.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Hands files that arrived through ACTION_SEND / ACTION_SEND_MULTIPLE / ACTION_VIEW from
 * MainActivity to the workbench ViewModel.
 *
 * replay = 1 keeps a cold-start intent until the UI subscribes; the ViewModel calls
 * [consume] right after taking the batch so it is handled exactly once.
 */
object IncomingFilesBus {

    private val _events = MutableSharedFlow<List<Uri>>(
        replay = 1,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<List<Uri>> = _events.asSharedFlow()

    fun offer(uris: List<Uri>) {
        val clean = uris.distinct()
        if (clean.isNotEmpty()) _events.tryEmit(clean)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun consume() {
        _events.resetReplayCache()
    }
}

/**
 * Extracts incoming file URIs from a share or view intent. ClipData covers senders that only
 * fill in the clip data, which some OEM gallery apps do.
 */
fun extractIncomingUris(intent: Intent?): List<Uri> {
    if (intent == null) return emptyList()
    val direct: List<Uri?> = when (intent.action) {
        Intent.ACTION_SEND -> listOf(
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        )
        Intent.ACTION_SEND_MULTIPLE -> {
            val list = IntentCompat.getParcelableArrayListExtra(
                intent, Intent.EXTRA_STREAM, Uri::class.java
            )
            list?.toList().orEmpty()
        }
        Intent.ACTION_VIEW -> listOf(intent.data)
        else -> emptyList()
    }
    val clip: List<Uri?> = intent.clipData?.let { data ->
        (0 until data.itemCount).map { index -> data.getItemAt(index).uri }
    }.orEmpty()
    return (direct + clip).filterNotNull().distinct()
}

/**
 * Gives back a persisted read grant once the batch that needed it has finished. Android caps
 * the number of persisted grants per app, so keeping every shared file forever would eventually
 * make new grants fail.
 */
fun releasePersistableReadPermission(context: Context, uri: Uri) {
    try {
        context.contentResolver.releasePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (_: SecurityException) {
        // The grant was transient (or already released); nothing to give back.
    } catch (_: Exception) {
        // Providers may throw for URIs they no longer own; releasing is best-effort.
    }
}

/**
 * Asks for a persistable read grant. Share intents do not offer one, so a SecurityException
 * here is expected and harmless: the transient grant still covers this session.
 */
fun takePersistableReadPermission(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: SecurityException) {
        // Not a persistable grant; the transient permission is enough.
    } catch (_: Exception) {
        // Some providers throw IllegalArgumentException for unrelated reasons.
    }
}

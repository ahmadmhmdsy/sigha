package com.sigha.converter.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri

/**
 * Reads one fact from a media container before any conversion starts: does this video have an
 * audio track? The workbench uses it to hide audio targets that are guaranteed to fail on a
 * silent video.
 *
 * Best-effort by design: any failure (unsupported container, revoked permission, broken provider)
 * returns null, and callers treat null as "unknown, keep offering the targets". This must never
 * surface an error to the user or block adding a file.
 */
internal object MediaTrackProbe {

    fun hasAudioTrack(context: Context, uri: Uri): Boolean? = try {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            (0 until extractor.trackCount).any { index ->
                val mime = extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
                mime?.startsWith("audio/") == true
            }
        } finally {
            runCatching { extractor.release() }
        }
    } catch (_: Throwable) {
        // A content provider may refuse openInputStream-style access, the file may be gone, or
        // the container may be corrupt; the conversion will report that properly if it matters.
        null
    }
}

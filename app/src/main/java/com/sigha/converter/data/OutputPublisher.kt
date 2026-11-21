package com.sigha.converter.data

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.BaseColumns
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.sigha.converter.R
import com.sigha.converter.core.ConversionException
import com.sigha.converter.core.ConversionFailureKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Where a finished conversion should be saved. */
sealed interface PublishTarget {
    /** Public Downloads/Sigha. */
    data object Downloads : PublishTarget

    /** A folder the user picked through ACTION_OPEN_DOCUMENT_TREE. */
    data class Folder(val treeUri: Uri) : PublishTarget
}

/** One file that was written to shared storage. */
data class PublishedOutput(
    val fileName: String,
    val mime: String,
    val uri: Uri,
    /** Human-readable location when known, e.g. "Download/Sigha/report.pdf". */
    val displayPath: String?,
    val sizeBytes: Long
)

/**
 * Copies finished cache files to a place the user keeps.
 *
 * API 29+ uses MediaStore Downloads (scoped storage, no permission needed).
 * API 26-28 writes to the public Downloads directory (runtime permission required) and asks
 * the media scanner for a content:// URI so other apps can still open the result.
 */
class OutputPublisher(private val context: Context) {

    fun hasWriteAccess(target: PublishTarget): Boolean = runCatching {
        when (target) {
            PublishTarget.Downloads -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            is PublishTarget.Folder ->
                DocumentFile.fromTreeUri(context, target.treeUri)?.canWrite() == true
        }
    }.getOrDefault(false)

    suspend fun publish(
        source: File,
        fileName: String,
        mime: String,
        target: PublishTarget
    ): PublishedOutput = withContext(Dispatchers.IO) {
        if (!source.isFile || source.length() <= 0L) {
            throw ConversionException(
                context.getString(R.string.err_save_failed),
                kind = ConversionFailureKind.SAVE_FAILED
            )
        }
        val safeMime = mime.ifBlank { DEFAULT_MIME }
        when (target) {
            PublishTarget.Downloads ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    publishToMediaStore(source, fileName, safeMime)
                } else {
                    publishToLegacyDownloads(source, fileName, safeMime)
                }
            is PublishTarget.Folder -> publishToFolder(source, fileName, safeMime, target.treeUri)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun publishToMediaStore(source: File, fileName: String, mime: String): PublishedOutput {
        val resolver = context.contentResolver
        val relativePath = Environment.DIRECTORY_DOWNLOADS + "/" + SUBFOLDER
        val displayName = UniqueNames.unique(fileName) { candidate ->
            mediaStoreNameExists(resolver, candidate, relativePath)
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        var uri: Uri? = null
        try {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw ConversionException(
                    context.getString(R.string.err_save_failed),
                    kind = ConversionFailureKind.SAVE_FAILED
                )
            resolver.openOutputStream(uri)?.use { output ->
                copyCancellable(source, output)
            } ?: throw ConversionException(
                context.getString(R.string.err_save_failed),
                kind = ConversionFailureKind.SAVE_FAILED
            )
            val finished = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            resolver.update(uri, finished, null, null)
        } catch (error: Exception) {
            uri?.let { pending -> runCatching { resolver.delete(pending, null, null) } }
            if (error is ConversionException) throw error
            throw ConversionException(
                context.getString(R.string.err_save_failed),
                error,
                ConversionFailureKind.SAVE_FAILED
            )
        }
        val savedUri = uri
        // The provider may rename the entry; report the name it actually stored.
        val savedName = readBackDisplayName(resolver, savedUri) ?: displayName
        return PublishedOutput(
            fileName = savedName,
            mime = mime,
            uri = savedUri,
            displayPath = relativePath + "/" + savedName,
            sizeBytes = source.length()
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun mediaStoreNameExists(resolver: ContentResolver, name: String, relativePath: String): Boolean {
        val selection = MediaStore.Downloads.DISPLAY_NAME + " = ? AND (" +
            MediaStore.Downloads.RELATIVE_PATH + " = ? OR " +
            MediaStore.Downloads.RELATIVE_PATH + " = ?)"
        val args = arrayOf(name, relativePath, relativePath + "/")
        return try {
            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(BaseColumns._ID),
                selection,
                args,
                null
            )?.use { cursor -> cursor.moveToFirst() } == true
        } catch (_: Exception) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun publishToLegacyDownloads(source: File, fileName: String, mime: String): PublishedOutput {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw ConversionException(
                context.getString(R.string.err_storage_permission),
                kind = ConversionFailureKind.STORAGE_PERMISSION
            )
        }
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val directory = File(root, SUBFOLDER)
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw ConversionException(
                context.getString(R.string.err_save_failed),
                kind = ConversionFailureKind.SAVE_FAILED
            )
        }
        val displayName = UniqueNames.unique(fileName) { candidate -> File(directory, candidate).exists() }
        val destination = File(directory, displayName)
        try {
            destination.outputStream().use { output -> copyCancellable(source, output) }
        } catch (error: Exception) {
            runCatching { destination.delete() }
            throw ConversionException(
                context.getString(R.string.err_save_failed),
                error,
                ConversionFailureKind.SAVE_FAILED
            )
        }
        // The file is already on disk and visible to file managers even when the media scanner
        // is slow or never calls back on this ROM (some API 26-28 builds). Reporting SAVE_FAILED
        // there would make the user convert the same file again and collect duplicates.
        val uri = scanIntoMediaStore(destination, mime)
            ?: resolveMediaStoreUriByPath(destination)
            ?: Uri.fromFile(destination)
        return PublishedOutput(
            fileName = displayName,
            mime = mime,
            uri = uri,
            displayPath = Environment.DIRECTORY_DOWNLOADS + "/" + SUBFOLDER + "/" + displayName,
            sizeBytes = destination.length()
        )
    }

    private suspend fun scanIntoMediaStore(file: File, mime: String): Uri? = withTimeoutOrNull(SCAN_TIMEOUT_MS) {
        suspendCancellableCoroutine { continuation ->
            try {
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mime)) { _, uri ->
                    if (continuation.isActive) continuation.resume(uri)
                }
            } catch (error: Exception) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
    }

    /**
     * When the scanner does not call back in time the row often still exists: some ROMs index
     * the file lazily. Querying by path gives the result a content URI that other apps can
     * open, instead of the file:// URI that Android refuses to expose across apps.
     */
    @Suppress("DEPRECATION")
    private fun resolveMediaStoreUriByPath(file: File): Uri? = runCatching {
        val collection = MediaStore.Files.getContentUri("external")
        context.contentResolver.query(
            collection,
            arrayOf(BaseColumns._ID),
            MediaStore.Files.FileColumns.DATA + " = ?",
            arrayOf(file.absolutePath),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) ContentUris.withAppendedId(collection, cursor.getLong(0)) else null
        }
    }.getOrNull()

    private suspend fun publishToFolder(source: File, fileName: String, mime: String, treeUri: Uri): PublishedOutput {
        val directory = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw ConversionException(
                context.getString(R.string.err_output_folder),
                kind = ConversionFailureKind.OUTPUT_FOLDER_UNAVAILABLE
            )
        if (!directory.isDirectory || !directory.canWrite()) {
            throw ConversionException(
                context.getString(R.string.err_output_folder),
                kind = ConversionFailureKind.OUTPUT_FOLDER_UNAVAILABLE
            )
        }
        val taken = try {
            directory.listFiles().mapNotNull { it.name }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
        val displayName = UniqueNames.unique(fileName) { candidate -> candidate in taken }
        val document = directory.createFile(mime, displayName)
            ?: throw ConversionException(
                context.getString(R.string.err_save_failed),
                kind = ConversionFailureKind.SAVE_FAILED
            )
        try {
            context.contentResolver.openOutputStream(document.uri)?.use { output ->
                copyCancellable(source, output)
            } ?: throw ConversionException(
                context.getString(R.string.err_save_failed),
                kind = ConversionFailureKind.SAVE_FAILED
            )
        } catch (error: Exception) {
            runCatching { document.delete() }
            if (error is ConversionException) throw error
            throw ConversionException(
                context.getString(R.string.err_save_failed),
                error,
                ConversionFailureKind.SAVE_FAILED
            )
        }
        // A provider may adjust the created document's name; report what it really is.
        val savedName = document.name?.takeIf { it.isNotBlank() } ?: displayName
        // The SAF tree hides its real filesystem path; the folder's display name plus the file
        // name is the most specific destination the queue and history can honestly show.
        val folderName = directory.name?.takeIf { it.isNotBlank() }
        return PublishedOutput(
            fileName = savedName,
            mime = mime,
            uri = document.uri,
            displayPath = if (folderName != null) folderName + "/" + savedName else savedName,
            sizeBytes = source.length()
        )
    }

    /**
     * Copies with a cancellation check every buffer, so Cancel during a long save does not leave
     * a half-written file behind: the catch in each publish path deletes the partial entry.
     */
    private suspend fun copyCancellable(source: File, output: java.io.OutputStream) {
        source.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                kotlin.coroutines.coroutineContext.ensureActive()
                val read = input.read(buffer)
                if (read < 0) break
                output.write(buffer, 0, read)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun readBackDisplayName(resolver: ContentResolver, uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(MediaStore.Downloads.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull()

    private companion object {
        const val SUBFOLDER = "Sigha"
        const val DEFAULT_MIME = "application/octet-stream"
        const val BUFFER_SIZE = 64 * 1024
        const val SCAN_TIMEOUT_MS = 20_000L
    }
}

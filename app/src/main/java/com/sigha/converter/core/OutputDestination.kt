package com.sigha.converter.core

/**
 * The public collection a finished file belongs in.
 *
 * Routing by media type is what makes a converted picture appear in the gallery and a converted
 * clip in the video app instead of silently landing in Downloads, where photo and video pickers
 * on API 29+ do not look. Every kind keeps the app subfolder so files remain easy to find.
 */
enum class OutputDestination(
    /** Folder relative to shared storage, e.g. "Pictures/Sigha". */
    val folderName: String,
    /** Public directory name used by the API 26-28 legacy path. */
    val legacyDirectory: String
) {
    IMAGES("Pictures/Sigha", "Pictures"),
    VIDEO("Movies/Sigha", "Movies"),
    AUDIO("Music/Sigha", "Music"),
    DOWNLOADS("Download/Sigha", "Download");

    companion object {
        /** The MediaStore relative path including the app subfolder ("Pictures/Sigha"). */
        fun relativePath(destination: OutputDestination): String = destination.folderName
    }
}

/**
 * Picks the destination for one finished file. The target category wins over the MIME type
 * because a converter may report a generic type, but both agree for every built-in target; a MIME
 * prefix is still checked so an engine that only sets the MIME still routes correctly.
 *
 * Examples: PDF -> Downloads, PDF page -> Pictures, video -> extracted MP3 -> Music,
 * silent video -> GIF -> Pictures.
 */
fun destinationFor(category: FormatCategory, mime: String): OutputDestination = when {
    category == FormatCategory.IMAGE || mime.startsWith("image/", ignoreCase = true) ->
        OutputDestination.IMAGES
    category == FormatCategory.VIDEO || mime.startsWith("video/", ignoreCase = true) ->
        OutputDestination.VIDEO
    category == FormatCategory.AUDIO || mime.startsWith("audio/", ignoreCase = true) ->
        OutputDestination.AUDIO
    else -> OutputDestination.DOWNLOADS
}

package com.sigha.converter.model

import android.content.Context
import java.io.File

/**
 * Where packs live on disk. Everything is inside the app's private files directory, so the
 * data disappears with the app and no other app can read it.
 *
 * ```
 * files/models/<id>/                    <- installDir()
 * files/models/<id>/tessdata/<file>     <- files, when the pack has a subdirectory
 * files/models/<id>.<file>.part         <- one resumable partial download per expected file
 * ```
 */
internal object PackLayout {

    fun modelsRoot(context: Context): File = File(context.filesDir, "models")

    fun installDir(context: Context, pack: ModelPack): File =
        File(modelsRoot(context), pack.id)

    /** Directory that directly holds the pack files. */
    fun fileDir(context: Context, pack: ModelPack): File {
        val root = installDir(context, pack)
        val subdirectory = pack.subdirectory
        return if (subdirectory.isNullOrBlank()) root else File(root, subdirectory)
    }

    fun targetFile(context: Context, pack: ModelPack, file: PackFile): File =
        File(fileDir(context, pack), file.fileName)

    /**
     * One partial file per expected download. A per-file name is what makes a resumed
     * download safe: the bytes on disk can only ever belong to that exact file.
     */
    fun partFile(context: Context, pack: ModelPack, file: PackFile): File =
        File(modelsRoot(context), pack.id + "." + file.fileName + ".part")

    fun partFiles(context: Context, pack: ModelPack): List<File> =
        pack.files.map { file -> partFile(context, pack, file) }

    /** True when an interrupted download left bytes that the next attempt can resume. */
    fun hasPartialDownload(context: Context, pack: ModelPack): Boolean =
        partFiles(context, pack).any { part -> part.isFile && part.length() > 0L }

    /**
     * Cheap installed check for the UI: every file exists at its final size. The SHA-256 was
     * already verified at install time; re-hashing 28 MB on every recomposition would not be.
     */
    fun isInstalled(context: Context, pack: ModelPack): Boolean =
        pack.files.all { file -> isFileValid(context, pack, file) }

    /**
     * True when at least one expected file is in place at the right size. A partially
     * downloaded OCR pack can already recognise the language it has (English before Arabic,
     * for example) even though the card keeps offering Download to finish the pack.
     */
    fun hasInstalledFiles(context: Context, pack: ModelPack): Boolean =
        pack.files.any { file -> isFileValid(context, pack, file) }

    /** Expected file names that are present and complete; the source of installed languages. */
    fun installedFileNames(context: Context, pack: ModelPack): List<String> =
        pack.files.filter { file -> isFileValid(context, pack, file) }.map { it.fileName }

    private fun isFileValid(context: Context, pack: ModelPack, file: PackFile): Boolean {
        val target = targetFile(context, pack, file)
        return target.isFile && target.length() == file.sizeBytes
    }
}

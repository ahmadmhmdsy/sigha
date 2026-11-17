package com.sigha.converter.model

import android.content.Context
import androidx.annotation.StringRes
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Downloadable model packs - the offline extras whose data is too large to ship in the APK.
 *
 * The model packs screen is written against exactly these members, and nothing here touches
 * the network until [download] runs from an explicit user tap. A pack is *data only*
 * (Tesseract traineddata today); no code is ever downloaded.
 *
 * Installed files stay in app-private storage and survive app updates:
 * `files/models/<id>/tessdata/<name>.traineddata` (Tesseract's data-path layout).
 */
object ModelPacks {

    /**
     * What the pack card renders. Title and description are string resource ids because the
     * screen (not the model) knows the locale; [licenseUrl] is opened by the license button.
     */
    data class PackInfo(
        val id: String,
        @StringRes val titleRes: Int,
        @StringRes val descriptionRes: Int,
        val sizeBytes: Long,
        val license: String,
        val licenseUrl: String
    )

    /** The OCR pack: English and Arabic traineddata from tessdata_best. */
    const val OCR_PACK_ID: String = "ocr"

    @Volatile
    private var appContext: Context? = null

    /**
     * Called once from AppGraph so the context-free parts of the API can answer.
     * Kept public because AppGraph owns the call site.
     */
    fun attach(context: Context) {
        appContext = context.applicationContext ?: context
    }

    fun catalog(): List<PackInfo> = PackCatalog.ALL.map { it.info() }

    fun isInstalled(id: String): Boolean {
        val pack = PackCatalog.byId(id) ?: return false
        val context = appContext ?: return false
        return PackLayout.isInstalled(context, pack)
    }

    /**
     * The pack's root directory once at least one file is installed, or null while there is
     * nothing to use. [isInstalled] stays the stricter "every file is present" check the card
     * shows, so a partly downloaded OCR pack is usable for the language it already has while
     * the UI still offers Download to finish it.
     *
     * For the OCR pack this is the data path handed to Tesseract; the traineddata files sit in
     * its `tessdata` child folder, exactly where Tesseract expects them.
     */
    fun installDir(context: Context, id: String): File? {
        val app = remember(context)
        val pack = PackCatalog.byId(id) ?: return null
        return if (PackLayout.hasInstalledFiles(app, pack)) PackLayout.installDir(app, pack) else null
    }

    /**
     * Downloads, verifies and installs the pack. Progress is 0f..1f and is delivered on the
     * IO thread; throwing [CancellationException] from [onProgress] aborts the download and
     * removes the temporary file.
     */
    suspend fun download(
        context: Context,
        id: String,
        onProgress: (Float) -> Unit
    ): Result<File> {
        val app = remember(context)
        val pack = PackCatalog.byId(id)
            ?: return Result.failure(PackException("Unknown model pack."))
        return try {
            val installed = withContext(Dispatchers.IO) {
                // The blocking download loop only yields through onProgress, so make every
                // tick observe coroutine cancellation as well as a throwing callback.
                val jobContext = coroutineContext
                val report: (Float) -> Unit = { value ->
                    jobContext.ensureActive()
                    onProgress(value)
                }
                PackInstaller(app).install(pack, report)
            }
            Result.success(installed)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    /** True when an interrupted download left resumable bytes for this pack. */
    fun hasPartialDownload(id: String): Boolean {
        val pack = PackCatalog.byId(id) ?: return false
        val context = appContext ?: return false
        return PackLayout.hasPartialDownload(context, pack)
    }

    /** Installed OCR languages in a stable order; empty when no traineddata is present. */
    fun installedOcrLanguages(context: Context): List<String> {
        val app = remember(context)
        val pack = PackCatalog.byId(OCR_PACK_ID) ?: return emptyList()
        return OcrLanguages.fromFileNames(PackLayout.installedFileNames(app, pack))
    }

    private fun remember(context: Context): Context {
        val app = context.applicationContext ?: context
        appContext = app
        return app
    }
}

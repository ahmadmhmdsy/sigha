package com.sigha.converter.data

import android.content.Context
import com.sigha.converter.model.ModelPacks
import java.io.File
import kotlinx.coroutines.CancellationException

/**
 * Thin UI-facing wrapper around the model pack manager. Keeps the screen free of platform
 * details and tolerates a pack manager that cannot answer instead of crashing the UI.
 */
class ModelPackStore(private val context: Context) {

    fun catalog(): List<ModelPacks.PackInfo> =
        runCatching { ModelPacks.catalog() }.getOrDefault(emptyList())

    fun isInstalled(packId: String): Boolean =
        runCatching { ModelPacks.isInstalled(packId) }.getOrDefault(false)

    suspend fun download(packId: String, onProgress: (Float) -> Unit): Result<File> =
        try {
            ModelPacks.download(context, packId, onProgress)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }

    fun installedOcrLanguages(): List<String> =
        runCatching { ModelPacks.installedOcrLanguages(context) }.getOrDefault(emptyList())
}

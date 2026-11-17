package com.sigha.converter.data

import android.content.Context
import com.sigha.converter.model.ModelPacks
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State of one model pack download. */
sealed interface PackDownloadState {
    data object Idle : PackDownloadState
    data class Downloading(val progress: Float) : PackDownloadState
    data object Installed : PackDownloadState
    /** [message] is the English diagnostic; the screen shows a localized failure headline. */
    data class Failed(val message: String?) : PackDownloadState
}

/**
 * App-scoped model pack downloads: leaving the Model packs screen does not cancel the download,
 * and progress survives navigation and recomposition. Only one pack downloads at a time.
 */
class PackDownloads(private val context: Context) {

    private val _states = MutableStateFlow<Map<String, PackDownloadState>>(emptyMap())
    val states: StateFlow<Map<String, PackDownloadState>> = _states.asStateFlow()

    private var job: Job? = null
    private var activePackId: String? = null

    fun stateOf(packId: String): PackDownloadState =
        _states.value[packId] ?: PackDownloadState.Idle

    /** Starts a download; ignored while another pack is downloading. */
    fun start(packId: String) {
        if (job?.isActive == true) return
        activePackId = packId
        _states.update { it + (packId to PackDownloadState.Downloading(0f)) }
        job = AppScope.launch {
            val result = try {
                ModelPacks.download(context, packId) { value ->
                    val progress = value.coerceIn(0f, 1f)
                    _states.update { current ->
                        if (current[packId] is PackDownloadState.Downloading) {
                            current + (packId to PackDownloadState.Downloading(progress))
                        } else {
                            current
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                _states.update { it + (packId to PackDownloadState.Idle) }
                activePackId = null
                throw cancelled
            } catch (error: Exception) {
                Result.failure<File>(error)
            }
            _states.update {
                it + (
                    packId to if (result.isSuccess) {
                        PackDownloadState.Installed
                    } else {
                        PackDownloadState.Failed(result.exceptionOrNull()?.message)
                    }
                    )
            }
            activePackId = null
        }
    }

    /** Cancels the running download; already downloaded bytes stay on disk for a resume. */
    fun cancel() {
        job?.cancel()
        activePackId = null
    }
}

package io.mehow.audioclip

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arthenica.ffmpegkit.FFmpegKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.sink
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.util.UUID
import kotlin.coroutines.resume

class MainViewModel(
    private val filesDir: File,
) : ViewModel() {
    private val client = OkHttpClient()

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    fun downloadFile(url: HttpUrl) {
        _state.update { it.copy(download = DownloadedFile.InProgress) }
        viewModelScope.launch {
            Log.i("AudioClip", "Start file download")
            val request = Request.Builder().url(url).build()
            client.newCall(request)
                .await()
                .mapCatching { response ->
                    val body = response.body!!
                    val file = File(filesDir, "${UUID.randomUUID()}.mp3")
                    withContext(Dispatchers.IO) {
                        body.source().use { source ->
                            file.sink().use { sink ->
                                source.readAll(sink)
                            }
                        }
                    }
                    file
                }
                .onSuccess { file ->
                    Log.i("AudioClip", "Downloaded audio file")
                    _state.update { it.copy(download = DownloadedFile.Downloaded(file)) }
                }
                .onFailure { error ->
                    Log.e("AudioClip", "Failed to download audio file", error)
                    _state.update { it.copy(download = DownloadedFile.Failure) }
                }
        }
    }

    fun updateAudioUrl(url: String?) {
        _state.update { it.copy(audioUrl = url) }
    }

    fun updateClipStart(start: BigInteger?) {
        _state.update { it.copy(clipStart = start) }
    }

    fun updateClipEnd(end: BigInteger?) {
        _state.update { it.copy(clipEnd = end) }
    }

    fun clipFile(file: File, start: BigInteger, end: BigInteger) {
        _state.update { it.copy(clipped = ClippedFile.InProgress) }
        viewModelScope.launch {
            Log.i("AudioClip", "Start file clipping")
            runCatching {
                val length = end - start
                val outputFile = File(filesDir, "${UUID.randomUUID()}.mp3")
                withContext(Dispatchers.IO) {
                    FFmpegKit.execute("-ss $start -i $file -t $length -c copy $outputFile")
                }
                outputFile
            }
                .onSuccess { file ->
                    Log.i("AudioClip", "Clipped audio file")
                    _state.update { it.copy(clipped = ClippedFile.Clipped(file)) }
                }
                .onFailure { error ->
                    Log.e("AudioClip", "Failed to clip audio file", error)
                    _state.update { it.copy(clipped = ClippedFile.Failure) }
                }
        }
    }

    data class State(
        val audioUrl: String? = "https://traffic.libsyn.com/secure/cosmicskeptic/57_David_Deutsch_AUDIO_AMENDED.mp3",
        val download: DownloadedFile = DownloadedFile.NotDownloaded,
        val clipped: ClippedFile = ClippedFile.NotClipped,
        val clipStart: BigInteger? = null,
        val clipEnd: BigInteger? = null,
    ) {
        val isDownloadEnabled get() = !isDownloading && audioUrl?.toHttpUrlOrNull() != null
        val isDownloading get() = download is DownloadedFile.InProgress
        val downloadedFile get() = (download as? DownloadedFile.Downloaded)?.file

        val isClippingEnabled get() = download is DownloadedFile.Downloaded && !isClipping && clipStart != null && clipEnd != null
        val isClipping get() = clipped is ClippedFile.InProgress
        val clippedFile get() = (clipped as? ClippedFile.Clipped)?.file
    }

    sealed interface DownloadedFile {
        data object NotDownloaded : DownloadedFile
        data object InProgress : DownloadedFile
        data class Downloaded(val file: File) : DownloadedFile
        data object Failure : DownloadedFile
    }

    sealed interface ClippedFile {
        data object NotClipped : ClippedFile
        data object InProgress : ClippedFile
        data class Clipped(val file: File) : ClippedFile
        data object Failure : ClippedFile
    }
}

private suspend fun Call.await(): Result<Response> = suspendCancellableCoroutine { continuation ->
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            continuation.resume(Result.failure(e))
        }

        override fun onResponse(call: Call, response: Response) {
            continuation.resume(Result.success(response))
        }
    })

    continuation.invokeOnCancellation { cancel() }
}
package io.mehow.audioclip

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.mehow.audioclip.MainViewModel.ClippedFile
import io.mehow.audioclip.MainViewModel.DownloadedFile
import io.mehow.audioclip.ui.theme.AudioClipTheme
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel by viewModels<MainViewModel> {
            viewModelFactory {
                initializer {
                    MainViewModel(filesDir)
                }
            }
        }

        setContent {
            val state by viewModel.state.collectAsState()

            AudioClipTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                        ) {
                            TextField(
                                value = state.audioUrl.orEmpty(),
                                onValueChange = { viewModel.updateAudioUrl(it) },
                                placeholder = { Text(text = "Audio URL") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                enabled = state.isDownloadEnabled,
                                onClick = {
                                    state.audioUrl?.toHttpUrlOrNull()?.let { url ->
                                        viewModel.downloadFile(url)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                            ) {
                                Text(
                                    text = "Download file",
                                    maxLines = 1,
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                            ) {
                                Text(
                                    text = state.download.label,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                if (state.isDownloading) {
                                    LinearProgressIndicator(modifier = Modifier.weight(1f))
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                            ) {
                                TextField(
                                    value = state.clipStart?.toString().orEmpty(),
                                    onValueChange = { viewModel.updateClipStart(it.toBigIntegerOrNull()) },
                                    placeholder = { Text(text = "Start", maxLines = 1) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextField(
                                    value = state.clipEnd?.toString().orEmpty(),
                                    onValueChange = { viewModel.updateClipEnd(it.toBigIntegerOrNull()) },
                                    placeholder = { Text(text = "End", maxLines = 1) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Button(
                                enabled = state.isClippingEnabled,
                                onClick = {
                                    state.downloadedFile?.let { file ->
                                        state.clipStart?.let { start ->
                                            state.clipEnd?.let { end ->
                                                viewModel.clipFile(file, start, end)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                            ) {
                                Text(
                                    text = "Clip file",
                                    maxLines = 1,
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                            ) {
                                Text(
                                    text = state.clipped.label,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                if (state.isClipping) {
                                    LinearProgressIndicator(modifier = Modifier.weight(1f))
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            val clippedFile = state.clippedFile
                            Button(
                                enabled = clippedFile != null,
                                onClick = {
                                    if (clippedFile != null) {
                                        val uri = FileProvider.getUriForFile(
                                            this@MainActivity,
                                            "io.mehow.audioclip.provider",
                                            clippedFile,
                                        )
                                        val share = ShareCompat.IntentBuilder(this@MainActivity)
                                            .setType("audio/mp3")
                                            .setStream(uri)
                                            .setChooserTitle("Share clip")
                                            .intent
                                            .setData(uri)
                                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        startActivity(Intent.createChooser(share, "Share clip"))
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                            ) {
                                Text(
                                    text = "Share clip",
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val DownloadedFile.label
    get() = when (this) {
        is DownloadedFile.Downloaded -> "File downloaded"
        is DownloadedFile.Failure -> "Download failure"
        is DownloadedFile.InProgress -> "Downloading file"
        is DownloadedFile.NotDownloaded -> "File not downloaded"
    }

private val ClippedFile.label
    get() = when (this) {
        is ClippedFile.Clipped -> "File clipped"
        is ClippedFile.Failure -> "Clipping failure"
        is ClippedFile.InProgress -> "Clipping file"
        is ClippedFile.NotClipped -> "File not clipped"
    }

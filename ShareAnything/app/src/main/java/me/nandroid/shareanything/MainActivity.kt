package me.nandroid.shareanything

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.nandroid.shareanything.ui.theme.ShareAnythingTheme
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*


class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShareAnythingTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ShareAnything(intent.toShareAnythingData())
                }
            }
        }
    }

    private fun getTextFromIntent(intent: Intent): String? {
        return intent.getStringExtra(Intent.EXTRA_TEXT)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    private fun getImageClipDataContent(intent: Intent): File? {
        return (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let { uri ->
            return@let createTemporaryFileFromUri(uri)
        }
    }

    private fun getVideoClipDataContent(intent: Intent): File? {
        return (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let { uri ->
            return@let createTemporaryFileFromUri(uri)
        }
    }

    private fun createTemporaryFileFromUri(uri: Uri): File? {
        val fileExtension = getFileExtension(uri)
        val temporaryFile = File.createTempFile("${System.currentTimeMillis()}", ".$fileExtension")
        temporaryFile.deleteOnExit()
        return applicationContext.contentResolver.openInputStream(uri)?.use {
            it.copyTo(temporaryFile.outputStream())
            return@use temporaryFile
        }
    }

    private fun getFileExtension(uri: Uri) = MimeTypeMap.getSingleton().getExtensionFromMimeType(applicationContext.contentResolver.getType(uri))

    private fun getAllFiles(intent: Intent): List<File> {
        val images = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { images ->
            return@let images
        }
        val files = images?.map {
            createTemporaryFileFromUri(it)
        }
        return files?.filterNotNull() ?: emptyList()
    }

    sealed class ServerStatus(var statusMessage: String) {
        data class Running(val msg: String) : ServerStatus(msg)
        data class Stopped(val msg: String) : ServerStatus(msg)
        data class Error(val msg: String) : ServerStatus(msg)
    }

    @Composable
    fun ShareAnything(data: ShareAnythingData) {
        if (data !is ShareAnythingData.ShareAnythingEmpty) {
            LaunchedEffect(key1 = Unit, block = {
                lifecycleScope.launch(Dispatchers.IO){
                    startServer()
                }
            })
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            TextInputField()
            Button(modifier =
            Modifier
                .fillMaxWidth(0.5f)
                .padding(vertical = 20.dp)
                .align(Alignment.CenterHorizontally),
                onClick = { lifecycleScope.launch(Dispatchers.IO) { toggleServerState() } }) {
                Text(text = viewModel.currentStatus.statusMessage)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TextInputField() {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.textFieldColors(containerColor = MaterialTheme.colorScheme.background),
            shape = RoundedCornerShape(12.dp),
            onValueChange = {
                viewModel.content = it
            },
            label = { Text(text = "Content") },
            placeholder = { Text(text = "Content goes here...") },
            value = viewModel.content,
            maxLines = 5
        )
    }

    private fun Intent.toShareAnythingData(): ShareAnythingData {
        when (this.action) {
            Intent.ACTION_SEND -> {
                if (this.type == "text/plain") {
                    val textFromIntent = getTextFromIntent(this)
                    return if (textFromIntent == null) return ShareAnythingData.ShareAnythingEmpty else ShareAnythingData.ShareAnythingText(
                        textFromIntent
                    )
                } else if (this.type?.startsWith("image/") == true) {
                    val file = getImageClipDataContent(this)
                    return if (file == null) return ShareAnythingData.ShareAnythingEmpty else ShareAnythingData.ShareAnythingFile(file)
                } else if (this.type?.startsWith("video/") == true) {
                    val file = getVideoClipDataContent(this)
                    return if (file == null) return ShareAnythingData.ShareAnythingEmpty else ShareAnythingData.ShareAnythingFile(file)
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val files = getAllFiles(this).map { ShareAnythingData.ShareAnythingFile(it) }
                return ShareAnythingData.ShareAnythingMultipleFiles(files)
            }
        }
        return ShareAnythingData.ShareAnythingEmpty
    }

    private suspend fun toggleServerState() {
        try {
            val isNotRunning = viewModel.isServerRunning
            if (isNotRunning) {
                startServer()
            } else {
                viewModel.stopServer()
                viewModel.currentStatus = ServerStatus.Stopped("Start Server")
            }
        } catch (e: Exception) {
            viewModel.currentStatus = ServerStatus.Error(e.message.toString())
        }
    }

    private suspend fun startServer() {
        val wifiIPAddress = viewModel.getIPAddress(true)
        viewModel.currentStatus = ServerStatus.Running("Server running at $wifiIPAddress")
        viewModel.startServer()
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        ShareAnythingTheme {
            ShareAnything(intent.toShareAnythingData())
        }
    }
}

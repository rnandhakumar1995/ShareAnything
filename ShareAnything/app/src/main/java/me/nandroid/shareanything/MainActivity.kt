package me.nandroid.shareanything

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.nandroid.shareanything.ui.theme.ShareAnythingTheme
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
                    ShareAnything()
                    handleSharingFromOtherApps(intent)
                }
            }
        }
    }

    private fun handleSharingFromOtherApps(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    handleSendText(intent)
                } else if (intent.type?.startsWith("image/") == true) {
                    handleSendImage(intent)
                } else if (intent.type?.startsWith("video/") == true) {
                    handleSendVideo(intent)
                }
                lifecycleScope.launch(Dispatchers.IO) { startServer() }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                if (intent.type?.startsWith("image/") == true) {
                    handleSendMultipleImages(intent)
                } else if (intent.type?.startsWith("video/") == true) {
                    handleSendMultipleVideos(intent)
                }
                lifecycleScope.launch(Dispatchers.IO) { startServer() }
            }
            else -> {
            }
        }
    }

    private fun handleSendText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            viewModel.content = it
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleSharingFromOtherApps(intent)
    }

    private fun handleSendImage(intent: Intent) {
        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {

        }
    }

    private fun handleSendMultipleImages(intent: Intent) {
        intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.let { images ->

        }
    }

    private fun handleSendVideo(intent: Intent) {
        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {

        }
    }

    private fun handleSendMultipleVideos(intent: Intent) {
        intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.let { images ->
        }
    }

    sealed class ServerStatus(var statusMessage: String) {
        data class Running(val msg: String) : ServerStatus(msg)
        data class Stopped(val msg: String) : ServerStatus(msg)
        data class Error(val msg: String) : ServerStatus(msg)
    }

    @Composable
    fun ShareAnything() {
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
            label = { Text(text = "Content")},
            placeholder = { Text(text = "Content goes here...")},
            value = viewModel.content,
            maxLines = 5
        )
    }

    private suspend fun toggleServerState() {
        try {
            val isNotRunning = viewModel.currentStatus !is ServerStatus.Running
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
        val isNotRunning = viewModel.currentStatus !is ServerStatus.Running
        if (isNotRunning) {
            val wifiIPAddress = getIPAddress(true)
            viewModel.currentStatus = ServerStatus.Running("Server running at $wifiIPAddress")
            viewModel.startServer()
        }
    }

    private fun getIPAddress(useIPv4: Boolean): String? {
        try {
            val interfaces: List<NetworkInterface> = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in interfaces) {
                val addresses: List<InetAddress> = Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress) {
                        val hostAddress = address.hostAddress
                        val isIPv4 = (hostAddress?.indexOf(':') ?: return null) < 0
                        if (useIPv4) {
                            if (isIPv4) return hostAddress
                        } else {
                            if (!isIPv4) {
                                val delimiter = hostAddress.indexOf('%') // drop ip6 zone suffix
                                return if (delimiter < 0) hostAddress.uppercase(Locale.getDefault()) else hostAddress.substring(0, delimiter)
                                    .uppercase(Locale.getDefault())
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        } // for now eat exceptions
        return ""
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        ShareAnythingTheme {
            ShareAnything()
        }
    }
}

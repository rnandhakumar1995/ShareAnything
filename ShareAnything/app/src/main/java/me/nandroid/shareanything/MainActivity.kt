package me.nandroid.shareanything

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
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
                    Greeting()
                    handleSharingFromOtherApps(intent)
                }
            }
        }
    }

    private fun handleSharingFromOtherApps(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    handleSendText(intent) // Handle text being sent
                } else if (intent.type?.startsWith("image/") == true) {
                    handleSendImage(intent) // Handle single image being sent
                } else if (intent.type?.startsWith("video/") == true) {
                    handleSendVideo(intent) // Handle single image being sent
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                if (intent.type?.startsWith("image/") == true) {
                    handleSendMultipleImages(intent) // Handle multiple images being sent
                } else if (intent.type?.startsWith("video/") == true) {
                    handleSendMultipleVideos(intent) // Handle single image being sent
                }
            }
            else -> {
                // Handle other intents, such as being started from the home screen
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Greeting() {
        Column {
            TextInputField()
            Button(modifier =
            Modifier
                .fillMaxWidth(0.5f)
                .padding(vertical = 20.dp)
                .align(Alignment.CenterHorizontally),
                onClick = { lifecycleScope.launch(Dispatchers.IO) { startServer() } }) {
                Text(text = viewModel.currentStatus.statusMessage)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TextInputField() {
        TextField(modifier = Modifier.fillMaxWidth(), onValueChange = {
            viewModel.content = it
        }, value = viewModel.content, maxLines = 5)
    }

    private suspend fun startServer() {
        try {
            val isRunning = viewModel.currentStatus !is ServerStatus.Running
            if (isRunning) {
                val wifiIPAddress = getIPAddress(true)
                viewModel.currentStatus = ServerStatus.Running("Server running at $wifiIPAddress")
                viewModel.startServer()
            } else {
                viewModel.stopServer()
                viewModel.currentStatus = ServerStatus.Stopped("Start Server")
            }
        } catch (e: Exception) {
            viewModel.currentStatus = ServerStatus.Error(e.message.toString())
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
            Greeting()
        }
    }
}

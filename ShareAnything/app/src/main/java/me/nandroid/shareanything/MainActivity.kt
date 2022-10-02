package me.nandroid.shareanything

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
                }
            }
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
            TextField(modifier = Modifier.fillMaxWidth(), onValueChange = {
                viewModel.content = it
            }, value = viewModel.content)
            Button(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(vertical = 20.dp)
                    .align(Alignment.CenterHorizontally),
                onClick = {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val b = viewModel.currentStatus !is ServerStatus.Running
                            if (b) {
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
                }) {
                Text(text = viewModel.currentStatus.statusMessage)
            }
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

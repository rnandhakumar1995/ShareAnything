package me.nandroid.shareanything

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

class MainActivityViewModel : ViewModel() {
    var content by mutableStateOf("")
    var currentStatus by mutableStateOf<MainActivity.ServerStatus>(MainActivity.ServerStatus.Stopped("Start Server"))
    private var embeddedServer: NettyApplicationEngine? = null
    val isServerRunning
        get() = currentStatus !is MainActivity.ServerStatus.Running

    suspend fun startServer() {
        embeddedServer = embeddedServer(Netty, port = 8080) {
            routing {
                get("/") {
                    call.respondText(content)
                }
            }
        }
        embeddedServer?.start(wait = true)
    }

    fun getIPAddress(useIPv4: Boolean): String? {
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


    suspend fun stopServer() {
        embeddedServer?.stop()
    }
}
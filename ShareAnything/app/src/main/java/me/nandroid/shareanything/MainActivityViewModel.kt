package me.nandroid.shareanything

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.resolver.FileSystemResolver
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.mustache.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*


class MainActivityViewModel(private val app: android.app.Application) : AndroidViewModel(app) {
    var content by mutableStateOf("")
    var files = mutableListOf<java.io.File>()
    var currentStatus by mutableStateOf<MainActivity.ServerStatus>(MainActivity.ServerStatus.Stopped("Start Server"))
    private var embeddedServer: NettyApplicationEngine? = null
    val isServerRunning
        get() = currentStatus !is MainActivity.ServerStatus.Running

    data class File(val name: String, val size: String)

    suspend fun startServer() {
        embeddedServer = embeddedServer(Netty, port = 8080) {
            try {
                val copiedFile = File(app.applicationContext.filesDir, "index.hbs")
                app.applicationContext.assets.open("index.hbs").use { input ->
                    copiedFile.outputStream().use { output ->
                        input.copyTo(output, 1024)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            install(Mustache) {
                mustacheFactory = DefaultMustacheFactory(FileSystemResolver(app.applicationContext.filesDir))

            }
            routing {
                get("/") {
                    if (files.isEmpty()) {
                        call.respondText(content)
                    } else {
                        try {
                            val files = files.map {  file ->
                                File(file.name, "${(file.length()/1024)}KB")
                            }
                            val mustacheResponse = MustacheContent("index.hbs", HashMap<String, List<File>>().apply {
                                put("files", files)
                            })
                            call.respond(mustacheResponse)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                get("/get") {
                    val file = files.singleOrNull { it.name == call.request.queryParameters["name"] }
                    if (file != null){
                        call.respondFile(file)
                    }else{
                        call.respondText { "Unable to find the file daa" }
                    }
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
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

class MainActivityViewModel : ViewModel() {
    var content by mutableStateOf("")
    var currentStatus by mutableStateOf<MainActivity.ServerStatus>(MainActivity.ServerStatus.Stopped("Start Server"))
    private var embeddedServer: NettyApplicationEngine? = null
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

    suspend fun stopServer() {
        embeddedServer?.stop()
    }
}
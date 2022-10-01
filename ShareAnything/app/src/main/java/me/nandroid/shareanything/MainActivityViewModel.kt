package me.nandroid.shareanything

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivityViewModel: ViewModel() {
    fun startServer(content: String){
        viewModelScope.launch(context = Dispatchers.IO) {
            embeddedServer(Netty, port = 8080) {
                routing {
                    get("/") {
                        call.respondText(content ?: "No content found")
                    }
                }
            }.start(wait = true)
        }
    }

    fun onTextChange() {

    }
}
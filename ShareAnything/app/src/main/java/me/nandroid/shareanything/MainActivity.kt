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
import me.nandroid.shareanything.ui.theme.ShareAnythingTheme


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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Greeting() {
        Column {
            var content by remember { mutableStateOf("Hello") }
            var serverStatus by remember { mutableStateOf("Start Server") }
            TextField(modifier = Modifier.fillMaxWidth(), onValueChange = {
                content = it
            }, value = content)
            Button(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(vertical = 20.dp)
                    .align(Alignment.CenterHorizontally),
                onClick = {
                    serverStatus = "Server running..."
                    viewModel.startServer(content)
                }) {
                Text(text = serverStatus)
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        ShareAnythingTheme {
            Greeting()
        }
    }
}

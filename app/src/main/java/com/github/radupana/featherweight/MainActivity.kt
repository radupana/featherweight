package com.github.radupana.featherweight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.room.Room
import com.github.radupana.featherweight.data.FeatherweightDatabase


class MainActivity : ComponentActivity() {
    val db = Room.databaseBuilder(
        applicationContext,
        FeatherweightDatabase::class.java, "featherweight-db"
    ).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FeatherweightApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatherweightApp() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Featherweight") }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.padding(paddingValues)
        ) {
            Greeting("Featherweight!")
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(
        text = "Hello, $name 👋",
        style = MaterialTheme.typography.headlineMedium
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FeatherweightApp()
}

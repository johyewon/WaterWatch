package com.hanix.waterwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hanix.waterwatch.ui.theme.WaterWatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WaterWatchTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
                    Home(Modifier.padding(inner))
                }
            }
        }
    }
}

@Composable
private fun Home(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("WaterWatch")
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    WaterWatchTheme { Home() }
}

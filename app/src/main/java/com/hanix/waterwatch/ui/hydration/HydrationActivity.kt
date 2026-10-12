package com.hanix.waterwatch.ui.hydration

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hanix.waterwatch.ui.theme.WaterWatchTheme

class HydrationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WaterWatchTheme {
                ScreenContents()
            }
        }
    }

    @Composable
    fun ScreenContents() {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { contentPadding ->
            HydrationRoute(modifier = Modifier.padding(contentPadding))
        }
    }
}

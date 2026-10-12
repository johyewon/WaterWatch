package com.hanix.waterwatch.wear.ui.hydration

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

class WearHydrationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScreenContents()
        }
    }

    @Composable
    fun ScreenContents() {
        MaterialTheme {
            WearHydrationRoute()
        }
    }
}

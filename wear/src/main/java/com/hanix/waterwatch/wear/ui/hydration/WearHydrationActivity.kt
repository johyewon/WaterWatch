package com.hanix.waterwatch.wear.ui.hydration

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.compose.material3.MaterialTheme

class WearHydrationActivity : ComponentActivity() {

    // 화면이 꺼져 저전력(ambient)으로 들어가도 액티비티는 RESUMED 로 남아 계속 그리므로 따로 받아서 애니메이션을 멈춘다.
    private var isAmbient by mutableStateOf(false)

    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
            isAmbient = true
        }

        override fun onExitAmbient() {
            isAmbient = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(AmbientLifecycleObserver(this, ambientCallback))
        setContent {
            ScreenContents()
        }
    }

    @Composable
    fun ScreenContents() {
        MaterialTheme {
            WearHydrationRoute(isAmbient = isAmbient)
        }
    }
}

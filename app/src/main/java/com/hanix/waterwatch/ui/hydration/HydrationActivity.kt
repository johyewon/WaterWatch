package com.hanix.waterwatch.ui.hydration

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
        // 테마를 라이트로 고정해서 시스템 바 아이콘도 다크 모드와 상관없이 어두운 색으로 맞춘다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
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

package com.hanix.waterwatch.ui.rationale

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanix.waterwatch.ui.theme.WaterWatchTheme

/**
 * 수분 섭취 데이터 사용 고지 화면
 *
 * 권한 시트의 방침 링크 착지점. 매니페스트로만 진입한다.
 */
// ponytail: 실제 고지 문구는 스토어 등록 전에 채운다.
class PermissionsRationaleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WaterWatchTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
                    Column(
                        Modifier
                            .padding(inner)
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "수분 섭취 데이터 사용 안내",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }
    }
}

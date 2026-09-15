package com.hanix.waterwatch.wear.ui.hydration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.hanix.waterwatch.wear.locator.ServiceLocator

/** 한 잔 기록 단위(ml) */
private const val SERVING_ML = 250

private val SCREEN_PADDING = 16.dp

@Composable
fun WearHydrationRoute(modifier: Modifier = Modifier) {
    val viewModel: WearHydrationViewModel = viewModel {
        WearHydrationViewModel(wearHydrationDataSource = ServiceLocator.wearHydrationDataSource)
    }

    val totalMl by viewModel.todayTotalMl.collectAsStateWithLifecycle()
    val sendResult by viewModel.sendResult.collectAsStateWithLifecycle()

    WearHydrationScreen(
        totalMl = totalMl,
        sendFailed = sendResult?.isFailure == true,
        onRecordClick = { viewModel.recordIntake(SERVING_ML) },
        modifier = modifier
    )
}

@Composable
fun WearHydrationScreen(
    totalMl: Int?,
    sendFailed: Boolean = false,
    onRecordClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SCREEN_PADDING),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = totalMl?.let { "오늘 $it ml" } ?: "폰 연결 대기")

        Button(onClick = onRecordClick) {
            Text(text = "+$SERVING_ML ml")
        }

        if (sendFailed) {
            Text(text = "전송 실패 — 폰 연결 확인")
        }
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun WearHydrationScreenPreview() {
    MaterialTheme {
        WearHydrationScreen(totalMl = 750)
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun WearHydrationScreenWaitingPreview() {
    MaterialTheme {
        WearHydrationScreen(totalMl = null, sendFailed = true)
    }
}

package com.hanix.waterwatch.ui.hydration

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hanix.waterwatch.data.source.HealthConnectDataSource
import com.hanix.waterwatch.locator.ServiceLocator
import com.hanix.waterwatch.ui.theme.WaterWatchTheme

/** 한 잔 기록 단위(ml) */
private const val SERVING_ML = 250.0

private val SCREEN_PADDING = 24.dp

private val ITEM_SPACING = 12.dp

@Composable
fun HydrationRoute(
    modifier: Modifier = Modifier,
    viewModel: HydrationViewModel = viewModel {
        HydrationViewModel(hydrationRepository = ServiceLocator.hydrationRepository)
    }
) {
    val granted by viewModel.granted.collectAsStateWithLifecycle()
    val total by viewModel.total.collectAsStateWithLifecycle()
    val watchConnected by viewModel.watchConnected.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { result ->
        viewModel.onPermissionResult(result.containsAll(HealthConnectDataSource.HYDRATION_PERMISSIONS))
    }

    LaunchedEffect(Unit) {
        viewModel.requestPermissions.collect {
            permissionLauncher.launch(HealthConnectDataSource.REQUESTED_PERMISSIONS)
        }
    }

    HydrationScreen(
        healthConnectAvailable = viewModel.isHealthConnectAvailable,
        granted = granted,
        total = total,
        watchConnected = watchConnected,
        onRecordClick = { viewModel.recordIntake(SERVING_ML) },
        modifier = modifier
    )
}

@Composable
fun HydrationScreen(
    healthConnectAvailable: Boolean,
    granted: Boolean?,
    total: Result<Double?>?,
    watchConnected: Boolean?,
    onRecordClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(SCREEN_PADDING),
        verticalArrangement = Arrangement.spacedBy(ITEM_SPACING)
    ) {
        Text(text = "Health Connect", style = MaterialTheme.typography.titleLarge)

        if (healthConnectAvailable.not()) {
            Text(
                text = "이 기기에서 Health Connect 를 사용할 수 없음",
                style = MaterialTheme.typography.titleMedium
            )
        }

        total?.let {
            Text(text = it.toLabel(), style = MaterialTheme.typography.titleMedium)
        }

        if (granted == true) {
            Button(onClick = onRecordClick) {
                Text(text = "+${SERVING_ML.toInt()} ml")
            }
        }

        watchConnected?.let {
            Text(
                text = if (it) "워치 연결됨" else "연결된 워치 없음",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun Result<Double?>.toLabel(): String = fold(
    onSuccess = { ml -> if (ml == null) "기록 없음" else "오늘 총 섭취량 ${ml.toInt()} ml" },
    onFailure = { "조회 실패 — ${it.javaClass.simpleName}" }
)

@Preview(showBackground = true)
@Composable
fun HydrationScreenPreview() {
    WaterWatchTheme {
        HydrationScreen(
            healthConnectAvailable = true,
            granted = true,
            total = Result.success(750.0),
            watchConnected = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HydrationScreenEmptyPreview() {
    WaterWatchTheme {
        HydrationScreen(
            healthConnectAvailable = true,
            granted = true,
            total = Result.success(null),
            watchConnected = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HydrationScreenUnavailablePreview() {
    WaterWatchTheme {
        HydrationScreen(
            healthConnectAvailable = false,
            granted = false,
            total = null,
            watchConnected = null
        )
    }
}

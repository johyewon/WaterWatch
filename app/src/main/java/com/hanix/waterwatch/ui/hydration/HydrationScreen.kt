package com.hanix.waterwatch.ui.hydration

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hanix.waterwatch.data.source.HealthConnectDataSource
import com.hanix.waterwatch.designsystem.Spacing
import com.hanix.waterwatch.locator.ServiceLocator
import com.hanix.waterwatch.ui.theme.WaterWatchTheme
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 한 번 눌러서 바로 기록하는 용량(ml) */
private val QUICK_SERVINGS_ML = listOf(120.0, 250.0)

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)

/** 기록 토스트는 기록 버튼 바로 위에 뜬다. 프로토타입 위치 그대로 */
private val TOAST_BOTTOM_MARGIN = 150.dp

@Composable
fun HydrationRoute(
    modifier: Modifier = Modifier,
    viewModel: HydrationViewModel = viewModel {
        HydrationViewModel(hydrationRepository = ServiceLocator.hydrationRepository)
    }
) {
    val granted by viewModel.granted.collectAsStateWithLifecycle()
    val total by viewModel.total.collectAsStateWithLifecycle()
    val goalMl by viewModel.goalMl.collectAsStateWithLifecycle()
    val recordedToastMl by viewModel.recordedToastMl.collectAsStateWithLifecycle()

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
        // ponytail: 화면 진입 시점 날짜. 자정 넘겨 켜둔 채면 안 바뀜, 필요해지면 날짜 변경 브로드캐스트 구독.
        today = LocalDate.now(),
        total = total,
        goalMl = goalMl,
        recordedToastMl = recordedToastMl,
        onRecordClick = viewModel::recordIntake,
        onUndoClick = viewModel::undoLastIntake,
        modifier = modifier
    )
}

@Composable
fun HydrationScreen(
    healthConnectAvailable: Boolean,
    granted: Boolean?,
    today: LocalDate,
    total: Result<Double?>?,
    goalMl: Int?,
    recordedToastMl: Double? = null,
    onRecordClick: (ml: Double) -> Unit = {},
    onCustomInputClick: () -> Unit = {},
    onUndoClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Health Connect 집계는 1089.9999… 처럼 오차가 섞여 와서 버리지 않고 반올림한다.
    val totalMl = total?.getOrNull()?.roundToInt() ?: 0

    val progress = goalMl?.let { (totalMl.toFloat() / it).coerceIn(0f, 1f) } ?: 0f

    Box(modifier = modifier.fillMaxSize()) {
        HydrationBackground(progress = progress, modifier = Modifier.fillMaxSize())
        Duck(progress = progress, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.XLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            TextButton(onClick = onSettingsClick, modifier = Modifier.align(Alignment.End)) {
                Text(text = "설정")
            }

            Text(text = today.format(DATE_FORMATTER), style = MaterialTheme.typography.titleMedium)
            Text(text = "%,d ml".format(totalMl), style = MaterialTheme.typography.displayLarge)

            val failure = total?.exceptionOrNull()
            when {
                failure != null -> Text(text = "조회 실패 — ${failure.javaClass.simpleName}")
                goalMl != null -> Text(text = remainingLabel(totalMl, goalMl))
            }

            if (healthConnectAvailable.not()) {
                Text(text = "이 기기에서 Health Connect 를 사용할 수 없음")
            }

            Spacer(modifier = Modifier.weight(1f))

            val recordEnabled = granted == true
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                QUICK_SERVINGS_ML.forEach { ml ->
                    Button(
                        onClick = { onRecordClick(ml) },
                        enabled = recordEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "+${ml.toInt()}ml")
                    }
                }
                Button(
                    onClick = onCustomInputClick,
                    enabled = recordEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "직접입력")
                }
            }

            TextButton(onClick = onUndoClick, enabled = recordEnabled) {
                Text(text = "이전 기록 취소하기")
            }
        }

        if (recordedToastMl != null) {
            RecordedToast(
                ml = recordedToastMl,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = Spacing.Large, end = Spacing.Large, bottom = TOAST_BOTTOM_MARGIN)
            )
        }
    }
}

private fun remainingLabel(totalMl: Int, goalMl: Int): String =
    if (totalMl >= goalMl) "오늘 목표를 채웠어요" else "%,dml 남았어요".format(goalMl - totalMl)

private val PREVIEW_DATE = LocalDate.of(2026, 9, 29)

@Preview(showBackground = true)
@Composable
fun HydrationScreenPreview() {
    WaterWatchTheme {
        HydrationScreen(
            healthConnectAvailable = true,
            granted = true,
            today = PREVIEW_DATE,
            total = Result.success(1_750.0),
            goalMl = 2_000
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
            today = PREVIEW_DATE,
            total = Result.success(null),
            goalMl = 2_000
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
            today = PREVIEW_DATE,
            total = null,
            goalMl = 2_000
        )
    }
}

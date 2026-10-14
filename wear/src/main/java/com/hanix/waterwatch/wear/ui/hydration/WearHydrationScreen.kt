package com.hanix.waterwatch.wear.ui.hydration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.hanix.waterwatch.designsystem.BackgroundLightBottom
import com.hanix.waterwatch.designsystem.BackgroundLightMiddle
import com.hanix.waterwatch.designsystem.BackgroundLightTop
import com.hanix.waterwatch.designsystem.ButtonBorder
import com.hanix.waterwatch.designsystem.ButtonContainer
import com.hanix.waterwatch.designsystem.ButtonContent
import com.hanix.waterwatch.designsystem.ButtonIcon
import com.hanix.waterwatch.designsystem.Ink
import com.hanix.waterwatch.wear.R
import com.hanix.waterwatch.wear.data.source.TodayIntake
import com.hanix.waterwatch.wear.locator.ServiceLocator

// 크기는 디자인 프로토타입(갤럭시 워치 384px 보드)을 2배 해상도로 보고 절반으로 옮겼다.

/** 한 번 눌러서 바로 기록하는 용량(ml) */
private val QUICK_SERVINGS_ML = listOf(120, 250)

private val BACKGROUND = Brush.verticalGradient(
    0f to BackgroundLightTop,
    0.55f to BackgroundLightMiddle,
    1f to BackgroundLightBottom
)

private val HEADER_PADDING_TOP = 17.dp
private val HEADER_PADDING_HORIZONTAL = 20.dp

private val BUTTONS_PADDING_HORIZONTAL = 21.dp
private val BUTTONS_PADDING_BOTTOM = 36.dp
private val BUTTON_SPACING = 3.dp
private val BUTTON_HEIGHT = 48.dp
private val BUTTON_ICON_SIZE = 10.dp

@Composable
fun WearHydrationRoute(modifier: Modifier = Modifier) {
    val viewModel: WearHydrationViewModel = viewModel {
        WearHydrationViewModel(wearHydrationDataSource = ServiceLocator.wearHydrationDataSource)
    }

    val todayIntake by viewModel.todayIntake.collectAsStateWithLifecycle()
    val sendResult by viewModel.sendResult.collectAsStateWithLifecycle()
    val recordAccepted by viewModel.recordAccepted.collectAsStateWithLifecycle()

    WearHydrationScreen(
        todayIntake = todayIntake,
        sendFailed = sendResult?.isFailure == true,
        recordFailed = recordAccepted == false,
        onRecordClick = viewModel::recordIntake,
        modifier = modifier
    )
}

/** [todayIntake] 가 null 이면 폰에서 아직 오늘 값을 받지 못한 상태 */
@Composable
fun WearHydrationScreen(
    todayIntake: TodayIntake?,
    sendFailed: Boolean = false,
    recordFailed: Boolean = false,
    onRecordClick: (ml: Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BACKGROUND)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = HEADER_PADDING_TOP, start = HEADER_PADDING_HORIZONTAL, end = HEADER_PADDING_HORIZONTAL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (todayIntake == null) {
                Text(text = "폰 연결 대기", color = Ink)
            } else {
                TotalMl(totalMl = todayIntake.totalMl)
                Text(
                    text = remainingLabel(todayIntake),
                    color = Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            when {
                sendFailed -> Text(text = "전송 실패 — 폰 연결 확인", color = Ink)
                recordFailed -> Text(text = "폰에서 기록 실패", color = Ink)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = BUTTONS_PADDING_HORIZONTAL,
                    end = BUTTONS_PADDING_HORIZONTAL,
                    bottom = BUTTONS_PADDING_BOTTOM
                ),
            horizontalArrangement = Arrangement.spacedBy(BUTTON_SPACING)
        ) {
            QUICK_SERVINGS_ML.forEach { ml ->
                QuickServingButton(
                    ml = ml,
                    onClick = { onRecordClick(ml) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TotalMl(totalMl: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "%,d".format(totalMl),
            color = Ink,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.alignByBaseline()
        )
        Text(text = "ml", color = Ink, fontSize = 14.sp, modifier = Modifier.alignByBaseline())
    }
}

@Composable
private fun QuickServingButton(ml: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(BUTTON_HEIGHT),
        colors = ButtonDefaults.buttonColors(containerColor = ButtonContainer, contentColor = ButtonContent),
        border = BorderStroke(1.dp, ButtonBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = null,
                tint = ButtonIcon,
                modifier = Modifier.size(BUTTON_ICON_SIZE)
            )
            Text(text = "$ml", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun remainingLabel(todayIntake: TodayIntake): String {
    val remainingMl = todayIntake.goalMl - todayIntake.totalMl
    return if (remainingMl <= 0) "목표를 채웠어요" else "%,dml 남았어요".format(remainingMl)
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun WearHydrationScreenPreview() {
    MaterialTheme {
        WearHydrationScreen(todayIntake = TodayIntake(totalMl = 800, goalMl = 2_000))
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun WearHydrationScreenWaitingPreview() {
    MaterialTheme {
        WearHydrationScreen(todayIntake = null, sendFailed = true)
    }
}

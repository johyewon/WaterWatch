package com.hanix.waterwatch.wear.ui.hydration

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.hanix.waterwatch.designsystem.ButtonBorder
import com.hanix.waterwatch.designsystem.ButtonContainer
import com.hanix.waterwatch.designsystem.ButtonContent
import com.hanix.waterwatch.designsystem.ButtonIcon
import com.hanix.waterwatch.designsystem.ButtonMoreIcon
import com.hanix.waterwatch.designsystem.HydrationBackground
import com.hanix.waterwatch.designsystem.HydrationBackgroundSpec
import com.hanix.waterwatch.designsystem.Ink
import com.hanix.waterwatch.designsystem.RollingNumber
import com.hanix.waterwatch.designsystem.headerBackgroundColor
import com.hanix.waterwatch.designsystem.readableTextColor
import com.hanix.waterwatch.wear.R
import com.hanix.waterwatch.wear.data.source.TodayIntake
import com.hanix.waterwatch.wear.locator.ServiceLocator
import com.hanix.waterwatch.wear.ui.theme.WearTypography

// 크기는 디자인 프로토타입(갤럭시 워치 384px 보드)을 2배 해상도로 보고 절반으로 옮겼다.

/** 한 번 눌러서 바로 기록하는 용량(ml) */
private val QUICK_SERVINGS_ML = listOf(120, 250)

private val HEADER_PADDING_TOP = 17.dp
private val HEADER_PADDING_HORIZONTAL = 20.dp

private val TOTAL_STYLE = TextStyle(
    fontSize = 24.sp,
    fontWeight = FontWeight.ExtraBold,
    letterSpacing = (-0.5).sp,
    lineHeight = 1.05.em
)
private const val LOADED_FADE_MILLIS = 250

// 헤더 위에 겹쳐 뜬다.
private val TOAST_PADDING_TOP = 50.dp

/** 물이 차올라 위쪽 배경이 어두워지면 글자색을 이 시간 동안 흰색으로 바꾼다. 프로토타입 값 */
private val INK_TWEEN = tween<Color>(durationMillis = 600)

private val BUTTONS_PADDING_BOTTOM = 36.dp
private val BUTTON_SPACING = 3.dp
// 보드에서는 세 칸 너비와 높이가 같아서 원이 된다. 큰 워치에서도 원을 유지하도록 크기를 고정한다.
private val BUTTON_SIZE = 48.dp
// 세 개가 한 줄에 들어가도록 기본 안쪽 여백을 없앤다(프로토타입 padding 0).
private val BUTTON_CONTENT_PADDING = PaddingValues(0.dp)
private val BUTTON_ICON_SIZE = 10.dp
private val MORE_ICON_SIZE = 18.dp

@Composable
fun WearHydrationRoute(isAmbient: Boolean, modifier: Modifier = Modifier) {
    val viewModel: WearHydrationViewModel = viewModel {
        WearHydrationViewModel(wearHydrationDataSource = ServiceLocator.wearHydrationDataSource)
    }

    val todayIntake by viewModel.todayIntake.collectAsStateWithLifecycle()
    val sendResult by viewModel.sendResult.collectAsStateWithLifecycle()
    val recordAccepted by viewModel.recordAccepted.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    WearHydrationScreen(
        todayIntake = todayIntake,
        sendFailed = sendResult?.isFailure == true,
        recordFailed = recordAccepted == false,
        toastMessage = toastMessage,
        onRecordClick = viewModel::recordIntake,
        onUndoClick = viewModel::undoLastIntake,
        isAmbient = isAmbient,
        modifier = modifier
    )
}

/**
 * [todayIntake] 가 null 이면 폰에서 아직 오늘 값을 받지 못한 상태.
 * [isAmbient] 면 화면은 그대로 두고 계속 도는 애니메이션만 멈춘다.
 */
@Composable
fun WearHydrationScreen(
    todayIntake: TodayIntake?,
    sendFailed: Boolean = false,
    recordFailed: Boolean = false,
    toastMessage: String? = null,
    onRecordClick: (ml: Int) -> Unit = {},
    onUndoClick: () -> Unit = {},
    isAmbient: Boolean = false,
    modifier: Modifier = Modifier
) {
    val progress = progressOf(todayIntake)
    val ink by animateColorAsState(
        readableTextColor(HydrationBackgroundSpec.Watch.headerBackgroundColor(progress), Ink),
        INK_TWEEN,
        label = "ink"
    )
    var showMore by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // ponytail: 저전력 화면은 디자인이 나오면 따로 만든다. 지금은 애니메이션만 멈춘다.
        HydrationBackground(
            progress = progress,
            spec = HydrationBackgroundSpec.Watch,
            animated = !isAmbient,
            modifier = Modifier.fillMaxSize()
        )
        ProgressRing(progress = progress, modifier = Modifier.fillMaxSize())
        WearDuck(progress = progress, animated = !isAmbient, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = HEADER_PADDING_TOP, start = HEADER_PADDING_HORIZONTAL, end = HEADER_PADDING_HORIZONTAL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 폰에서 값을 받기 전에는 0 이 보였다가 바뀌지 않도록 숨겨 두고, 받으면 나타나게 한다.
            // 숨기는 동안에도 자리는 차지해서 받은 뒤에 화면이 밀리지 않는다.
            val loaded = todayIntake != null
            val loadedAlpha by animateFloatAsState(if (loaded) 1f else 0f, tween(LOADED_FADE_MILLIS), label = "loadedAlpha")
            Column(
                modifier = Modifier.graphicsLayer { alpha = loadedAlpha },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 처음 받은 값은 굴리지 않는다. 받기 전(0)과 후를 다른 슬롯으로 그려서 새로 시작하게 한다.
                key(loaded) {
                    TotalMl(totalMl = todayIntake?.totalMl ?: 0, color = ink)
                }
                Text(
                    text = todayIntake?.let { remainingLabel(it) }.orEmpty(),
                    color = ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            when {
                sendFailed -> Text(text = "전송 실패 — 폰 연결 확인", color = ink)
                recordFailed -> Text(text = "폰에서 기록 실패", color = ink)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = BUTTONS_PADDING_BOTTOM),
            horizontalArrangement = Arrangement.spacedBy(BUTTON_SPACING)
        ) {
            QUICK_SERVINGS_ML.forEach { ml ->
                QuickServingButton(ml = ml, onClick = { onRecordClick(ml) })
            }
            MoreButton(onClick = { showMore = true })
        }

        if (toastMessage != null) {
            WearToast(
                message = toastMessage,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = TOAST_PADDING_TOP)
            )
        }
    }

    MoreDialog(
        visible = showMore,
        canUndo = (todayIntake?.totalMl ?: 0) > 0,
        onRecord = onRecordClick,
        onUndo = onUndoClick,
        onDismiss = { showMore = false }
    )
}

@Composable
private fun TotalMl(totalMl: Int, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
        RollingNumber(value = totalMl, style = TOTAL_STYLE, color = color, modifier = Modifier.alignByBaseline())
        Text(text = "ml", color = color, fontSize = 14.sp, modifier = Modifier.alignByBaseline())
    }
}

@Composable
private fun QuickServingButton(ml: Int, onClick: () -> Unit) {
    RoundButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = null,
            tint = ButtonIcon,
            modifier = Modifier.size(BUTTON_ICON_SIZE)
        )
        Text(text = "$ml", fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MoreButton(onClick: () -> Unit) {
    RoundButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_more),
            contentDescription = "더보기: 직접 입력, 이전 기록 취소",
            tint = ButtonMoreIcon,
            modifier = Modifier.size(MORE_ICON_SIZE)
        )
    }
}

/** 기록 줄의 흰 원형 버튼. 내용은 가운데에 가로로 놓는다. */
@Composable
private fun RoundButton(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(BUTTON_SIZE),
        colors = ButtonDefaults.buttonColors(containerColor = ButtonContainer, contentColor = ButtonContent),
        border = BorderStroke(1.dp, ButtonBorder),
        contentPadding = BUTTON_CONTENT_PADDING
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/** 목표 대비 오늘 섭취량(0..1). 아직 못 받았거나 목표가 0 이하면 0 */
private fun progressOf(todayIntake: TodayIntake?): Float {
    if (todayIntake == null || todayIntake.goalMl <= 0) {
        return 0f
    }
    return (todayIntake.totalMl.toFloat() / todayIntake.goalMl).coerceIn(0f, 1f)
}

private fun remainingLabel(todayIntake: TodayIntake): String {
    val remainingMl = todayIntake.goalMl - todayIntake.totalMl
    return if (remainingMl <= 0) "목표를 채웠어요" else "%,dml 남았어요".format(remainingMl)
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun WearHydrationScreenPreview() {
    MaterialTheme(typography = WearTypography) {
        WearHydrationScreen(todayIntake = TodayIntake(totalMl = 800, goalMl = 2_000))
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun WearHydrationScreenWaitingPreview() {
    MaterialTheme(typography = WearTypography) {
        WearHydrationScreen(todayIntake = null, sendFailed = true)
    }
}

package com.hanix.waterwatch.ui.hydration

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hanix.waterwatch.R
import com.hanix.waterwatch.data.source.HealthConnectDataSource
import com.hanix.waterwatch.designsystem.ButtonBorder
import com.hanix.waterwatch.designsystem.ButtonContainer
import com.hanix.waterwatch.designsystem.ButtonContent
import com.hanix.waterwatch.designsystem.HeaderText
import com.hanix.waterwatch.designsystem.HydrationBackground
import com.hanix.waterwatch.designsystem.HydrationBackgroundSpec
import com.hanix.waterwatch.designsystem.Ink
import com.hanix.waterwatch.designsystem.Spacing
import com.hanix.waterwatch.designsystem.UndoContent
import com.hanix.waterwatch.designsystem.footerBackgroundColor
import com.hanix.waterwatch.designsystem.headerBackgroundColor
import com.hanix.waterwatch.designsystem.readableTextColor
import com.hanix.waterwatch.locator.ServiceLocator
import com.hanix.waterwatch.ui.component.HydrationButton
import com.hanix.waterwatch.ui.component.HydrationButtonStyle
import com.hanix.waterwatch.ui.goal.GoalSheet
import com.hanix.waterwatch.ui.theme.WaterWatchTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** 한 번 눌러서 바로 기록하는 용량(ml) */
private val QUICK_SERVINGS_ML = listOf(120.0, 250.0)

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)

/*
 * 위쪽 위치는 커스텀 디자인 보드(높이 844)에서 화면 맨 위부터 잰 값을 화면 높이 비율로 옮긴다.
 * 고정값으로 두면 화면이 긴 기기에서 위에 붙어 보인다.
 */
private const val HEADER_TOP_RATIO = 76f / 844f
/** 브라우저 기본 줄 높이(normal)에 가깝게 맞춘다. */
private val HEADER_LINE_HEIGHT = 1.2.em

private const val SETTINGS_TOP_RATIO = 52f / 844f
private val SETTINGS_PADDING_END = 16.dp
private val SETTINGS_BUTTON_SIZE = 44.dp
private val SETTINGS_ICON_SIZE = 22.dp

private val TEXT_COLOR_TWEEN = tween<Color>(durationMillis = 600)

/** 토스트는 기록 버튼 바로 위에 뜬다. 프로토타입 위치 그대로 */
private val TOAST_BOTTOM_MARGIN = 150.dp

@Composable
fun HydrationRoute(
    modifier: Modifier = Modifier,
    firstEntry: Boolean = false,
    viewModel: HydrationViewModel = viewModel {
        HydrationViewModel(hydrationRepository = ServiceLocator.hydrationRepository, firstEntry = firstEntry)
    }
) {
    val granted by viewModel.granted.collectAsStateWithLifecycle()
    val total by viewModel.total.collectAsStateWithLifecycle()
    val goalMl by viewModel.goalMl.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    var showGoalSheet by rememberSaveable { mutableStateOf(false) }

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
        toastMessage = toastMessage,
        onRecordClick = viewModel::recordIntake,
        onUndoClick = viewModel::undoLastIntake,
        onSettingsClick = { showGoalSheet = true },
        modifier = modifier
    )

    // 목표를 읽기 전에는 시트에 넣을 값이 없어서 열지 않는다.
    val currentGoalMl = goalMl
    if (showGoalSheet && currentGoalMl != null) {
        GoalSheet(
            initialGoalMl = currentGoalMl,
            confirmText = "저장하기",
            note = "오늘 기록부터 바로 변경돼요.",
            onConfirm = { goal ->
                showGoalSheet = false
                viewModel.changeGoal(goal)
            },
            onDismissRequest = { showGoalSheet = false }
        )
    }
}

@Composable
fun HydrationScreen(
    healthConnectAvailable: Boolean,
    granted: Boolean?,
    today: LocalDate,
    total: Result<Double?>?,
    goalMl: Int?,
    toastMessage: String? = null,
    onRecordClick: (ml: Double) -> Unit = {},
    onCustomInputClick: () -> Unit = {},
    onUndoClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Health Connect 집계는 1089.9999… 처럼 오차가 섞여 와서 버리지 않고 반올림한다.
    val totalMl = total?.getOrNull()?.roundToInt() ?: 0

    val progress = goalMl?.let { (totalMl.toFloat() / it).coerceIn(0f, 1f) } ?: 0f
    val textColors = rememberTextColors(progress)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val headerTop = maxHeight * HEADER_TOP_RATIO
        val settingsTop = maxHeight * SETTINGS_TOP_RATIO

        HydrationBackground(progress = progress, spec = HydrationBackgroundSpec.Phone, modifier = Modifier.fillMaxSize())
        Duck(progress = progress, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                // 위쪽은 상태 바 높이와 상관없이 화면 높이에 대한 비율로 정한다.
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .padding(start = Spacing.XLarge, end = Spacing.XLarge, bottom = Spacing.XLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            Header(
                today = today,
                totalMl = totalMl,
                status = total?.exceptionOrNull()?.let { "조회 실패 — ${it.javaClass.simpleName}" }
                    ?: goalMl?.let { remainingLabel(totalMl, it) },
                healthConnectAvailable = healthConnectAvailable,
                textColors = textColors,
                modifier = Modifier.padding(top = headerTop)
            )

            Spacer(modifier = Modifier.weight(1f))

            val recordEnabled = granted == true
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                QUICK_SERVINGS_ML.forEach { ml ->
                    HydrationButton(
                        text = "${ml.toInt()}ml",
                        onClick = { onRecordClick(ml) },
                        style = HydrationButtonStyle.Outlined,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "${ml.toInt()}ml 마셨어요" },
                        enabled = recordEnabled,
                        icon = painterResource(R.drawable.ic_plus)
                    )
                }
                HydrationButton(
                    text = "직접입력",
                    onClick = onCustomInputClick,
                    style = HydrationButtonStyle.Outlined,
                    modifier = Modifier.weight(1f),
                    enabled = recordEnabled
                )
            }

            TextButton(
                onClick = onUndoClick,
                enabled = recordEnabled,
                colors = ButtonDefaults.textButtonColors(contentColor = textColors.undo)
            ) {
                Text(text = "이전 기록 취소하기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        OutlinedIconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End))
                .padding(top = settingsTop, end = SETTINGS_PADDING_END)
                .size(SETTINGS_BUTTON_SIZE),
            colors = IconButtonDefaults.outlinedIconButtonColors(
                containerColor = ButtonContainer,
                contentColor = ButtonContent
            ),
            border = BorderStroke(1.dp, ButtonBorder)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = "설정: 하루 목표 바꾸기",
                modifier = Modifier.size(SETTINGS_ICON_SIZE)
            )
        }

        if (toastMessage != null) {
            HydrationToast(
                message = toastMessage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding()
                    .padding(start = Spacing.Large, end = Spacing.Large, bottom = TOAST_BOTTOM_MARGIN)
            )
        }
    }
}

/** 날짜, 오늘 마신 양, 남은 양. 크기·굵기는 디자인 프로토타입 값 그대로. */
@Composable
private fun Header(
    today: LocalDate,
    totalMl: Int,
    status: String?,
    healthConnectAvailable: Boolean,
    textColors: TextColors,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {
        Text(
            text = today.format(DATE_FORMATTER),
            color = textColors.header,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = HEADER_LINE_HEIGHT
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "%,d".format(totalMl),
                modifier = Modifier.alignByBaseline(),
                color = textColors.amount,
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-2).sp,
                lineHeight = 1.05.em,
                // 숫자가 바뀌어도 폭이 흔들리지 않게 고정폭 숫자를 쓴다.
                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
            )
            Text(
                text = "ml",
                modifier = Modifier.alignByBaseline(),
                color = textColors.amount,
                fontSize = 26.sp,
                lineHeight = HEADER_LINE_HEIGHT
            )
        }
        status?.let { StatusText(it, textColors.header) }
        if (healthConnectAvailable.not()) {
            StatusText("이 기기에서 Health Connect 를 사용할 수 없음", textColors.header)
        }
    }
}

@Composable
private fun StatusText(text: String, color: Color) {
    Text(text = text, color = color, fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = HEADER_LINE_HEIGHT)
}

/** 물 위에 놓인 글자색. 배경이 어두워지면 흰색으로 바뀐다. */
private class TextColors(val header: Color, val amount: Color, val undo: Color)

/** 진행률이 바뀌면 프로토타입처럼 0.6초 동안 글자색을 바꾼다. */
@Composable
private fun rememberTextColors(progress: Float): TextColors {
    val headerOnDark = readableTextColor(HydrationBackgroundSpec.Phone.headerBackgroundColor(progress), Ink) == Color.White
    val header by animateColorAsState(if (headerOnDark) Color.White else HeaderText, TEXT_COLOR_TWEEN, label = "header")
    val amount by animateColorAsState(if (headerOnDark) Color.White else Ink, TEXT_COLOR_TWEEN, label = "amount")
    val undo by animateColorAsState(
        readableTextColor(footerBackgroundColor(progress), UndoContent),
        TEXT_COLOR_TWEEN,
        label = "undo"
    )
    return TextColors(header = header, amount = amount, undo = undo)
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

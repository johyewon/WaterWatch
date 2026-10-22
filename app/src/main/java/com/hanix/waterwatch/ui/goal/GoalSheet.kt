package com.hanix.waterwatch.ui.goal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanix.waterwatch.R
import com.hanix.waterwatch.designsystem.CaptionText
import com.hanix.waterwatch.designsystem.Spacing
import com.hanix.waterwatch.designsystem.SubtleSurface
import com.hanix.waterwatch.designsystem.TitleText
import com.hanix.waterwatch.ui.component.HydrationBottomSheet
import kotlin.math.roundToInt

// 크기는 디자인 프로토타입 값 그대로. 색은 :designsystem 에서 가져온다.

private val STEPPER_PADDING_TOP = 28.dp
private val STEPPER_PADDING_HORIZONTAL = 12.dp
private val STEP_BUTTON_SIZE = 48.dp
private val STEP_ICON_SIZE = 22.dp
private const val DISABLED_ALPHA = 0.4f

private const val ROLL_MILLIS = 220
private val ROLL_TWEEN = tween<IntOffset>(ROLL_MILLIS)

// 자리마다 따로 그려도 폭이 흔들리지 않게 고정폭 숫자(tnum)를 쓴다.
private val GOAL_NUMBER_STYLE = TextStyle(
    color = TitleText,
    fontSize = 40.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-1).sp,
    fontFeatureSettings = "tnum"
)

private const val GOAL_STEP_ML = 100
private val GOAL_RANGE_ML = 500..4_000

/**
 * 하루 목표를 100ml 단위로 고르는 시트. 온보딩과 메인 설정이 같이 쓰고 버튼 문구·안내만 다르다.
 * 고른 값은 확인을 눌러야 [onConfirm] 으로 나간다. 닫으면 버린다.
 */
@Composable
fun GoalSheet(
    initialGoalMl: Int,
    confirmText: String,
    onConfirm: (goalMl: Int) -> Unit,
    onDismissRequest: () -> Unit,
    note: String? = null
) {
    var goalMl by rememberSaveable { mutableIntStateOf(initialGoalMl.coerceIn(GOAL_RANGE_ML)) }

    HydrationBottomSheet(
        title = "하루 목표 설정",
        confirmText = confirmText,
        onConfirm = { onConfirm(goalMl) },
        onDismissRequest = onDismissRequest
    ) {
        if (note != null) {
            Text(
                text = note,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.Small),
                color = CaptionText,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center
            )
        }
        GoalStepper(
            goalMl = goalMl,
            onGoalChange = { goalMl = it },
            modifier = Modifier.padding(
                start = STEPPER_PADDING_HORIZONTAL,
                top = STEPPER_PADDING_TOP,
                end = STEPPER_PADDING_HORIZONTAL
            )
        )
    }
}

@Composable
private fun GoalStepper(goalMl: Int, onGoalChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepButton(
            iconRes = R.drawable.ic_stepper_minus,
            contentDescription = "100ml 줄이기",
            enabled = goalMl > GOAL_RANGE_ML.first,
            onClick = { onGoalChange(goalMl - GOAL_STEP_ML) }
        )
        Row(
            modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "하루 목표 %,dml".format(goalMl) },
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RollingNumber(value = goalMl, modifier = Modifier.alignByBaseline())
            Text(text = "ml", modifier = Modifier.alignByBaseline(), color = TitleText, fontSize = 20.sp)
        }
        StepButton(
            iconRes = R.drawable.ic_stepper_plus,
            contentDescription = "100ml 늘리기",
            enabled = goalMl < GOAL_RANGE_ML.last,
            onClick = { onGoalChange(goalMl + GOAL_STEP_ML) }
        )
    }
}

/**
 * 숫자가 바뀌면 바뀐 자리만 슬롯처럼 굴러간다. 늘면 아래에서 올라오고 줄면 위에서 내려온다.
 * 자리는 오른쪽부터 세어 묶어서, 1,000 처럼 자릿수가 늘어도 일의 자리부터 같은 슬롯을 쓴다.
 */
@Composable
private fun RollingNumber(value: Int, modifier: Modifier = Modifier) {
    var previousValue by remember { mutableIntStateOf(value) }
    val increasing = value >= previousValue
    SideEffect { previousValue = value }

    val style = LocalTextStyle.current.merge(GOAL_NUMBER_STYLE)
    val textMeasurer = rememberTextMeasurer()
    // 굴러가는 동안 칸 밖으로 밀려난 숫자의 글자 밑선이 잡혀 옆 "ml" 이 튀지 않도록, 밑선을 고정값으로 알려준다.
    val baseline = remember(textMeasurer, style) { textMeasurer.measure("0", style).firstBaseline.roundToInt() }

    val text = "%,d".format(value)
    Row(
        modifier = modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height, mapOf(FirstBaseline to baseline)) {
                placeable.place(0, 0)
            }
        }
    ) {
        text.forEachIndexed { index, char ->
            key(text.length - index) {
                AnimatedContent(
                    targetState = char,
                    // 슬롯 창처럼 자기 칸 밖은 보이지 않게 자른다.
                    modifier = Modifier.clipToBounds(),
                    transitionSpec = {
                        val direction = if (increasing) 1 else -1
                        slideInVertically(ROLL_TWEEN) { height -> height * direction }
                            .togetherWith(slideOutVertically(ROLL_TWEEN) { height -> -height * direction })
                    },
                    label = "goalDigit"
                ) { digit ->
                    Text(text = digit.toString(), style = style)
                }
            }
        }
    }
}

/** 끝값에 닿으면 프로토타입처럼 전체를 흐리게 하고 막는다. */
@Composable
private fun StepButton(iconRes: Int, contentDescription: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(STEP_BUTTON_SIZE)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .background(SubtleSurface, CircleShape)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = TitleText,
            modifier = Modifier.size(STEP_ICON_SIZE)
        )
    }
}

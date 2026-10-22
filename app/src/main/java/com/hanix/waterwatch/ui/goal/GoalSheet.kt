package com.hanix.waterwatch.ui.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanix.waterwatch.R
import com.hanix.waterwatch.designsystem.CaptionText
import com.hanix.waterwatch.designsystem.RollingNumber
import com.hanix.waterwatch.designsystem.Spacing
import com.hanix.waterwatch.designsystem.SubtleSurface
import com.hanix.waterwatch.designsystem.TitleText
import com.hanix.waterwatch.ui.component.HydrationBottomSheet

// 크기는 디자인 프로토타입 값 그대로. 색은 :designsystem 에서 가져온다.

private val STEPPER_PADDING_TOP = 28.dp
private val STEPPER_PADDING_HORIZONTAL = 12.dp
private val STEP_BUTTON_SIZE = 48.dp
private val STEP_ICON_SIZE = 22.dp
private const val DISABLED_ALPHA = 0.4f

private val GOAL_NUMBER_STYLE = TextStyle(
    fontSize = 40.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-1).sp
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
            RollingNumber(value = goalMl, style = GOAL_NUMBER_STYLE, color = TitleText, modifier = Modifier.alignByBaseline())
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

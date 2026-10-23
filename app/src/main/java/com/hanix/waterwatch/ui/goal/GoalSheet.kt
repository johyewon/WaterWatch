package com.hanix.waterwatch.ui.goal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanix.waterwatch.designsystem.CaptionText
import com.hanix.waterwatch.designsystem.Spacing
import com.hanix.waterwatch.ui.component.AmountStepper
import com.hanix.waterwatch.ui.component.HydrationBottomSheet

// 크기는 디자인 프로토타입 값 그대로. 색은 :designsystem 에서 가져온다.

private val STEPPER_PADDING_TOP = 28.dp
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
        AmountStepper(
            amountMl = goalMl,
            range = GOAL_RANGE_ML,
            onAmountChange = { goalMl = it },
            label = "하루 목표",
            modifier = Modifier.padding(top = STEPPER_PADDING_TOP)
        )
    }
}

package com.hanix.waterwatch.ui.component

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanix.waterwatch.R
import com.hanix.waterwatch.designsystem.RollingNumber
import com.hanix.waterwatch.designsystem.SubtleSurface
import com.hanix.waterwatch.designsystem.TitleText

// 크기는 디자인 프로토타입 값 그대로. 색은 :designsystem 에서 가져온다.

private val STEPPER_PADDING_HORIZONTAL = 12.dp
private val STEP_BUTTON_SIZE = 48.dp
private val STEP_ICON_SIZE = 22.dp
private const val DISABLED_ALPHA = 0.4f

private const val AMOUNT_STEP_ML = 100

private val AMOUNT_STYLE = TextStyle(
    fontSize = 40.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-1).sp
)

/**
 * 시트 안에서 양을 [AMOUNT_STEP_ML] 단위로 고르는 −/+ 줄. 하루 목표와 마신 양 직접 입력이 같이 쓴다.
 * @param label 화면 읽기 프로그램이 숫자와 함께 읽는 이름. 예) "하루 목표"
 */
@Composable
fun AmountStepper(
    amountMl: Int,
    range: IntRange,
    onAmountChange: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = STEPPER_PADDING_HORIZONTAL),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepButton(
            iconRes = R.drawable.ic_stepper_minus,
            contentDescription = "${AMOUNT_STEP_ML}ml 줄이기",
            enabled = amountMl > range.first,
            onClick = { onAmountChange((amountMl - AMOUNT_STEP_ML).coerceIn(range)) }
        )
        Row(
            modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$label %,dml".format(amountMl) },
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RollingNumber(value = amountMl, style = AMOUNT_STYLE, color = TitleText, modifier = Modifier.alignByBaseline())
            Text(text = "ml", modifier = Modifier.alignByBaseline(), color = TitleText, fontSize = 20.sp)
        }
        StepButton(
            iconRes = R.drawable.ic_stepper_plus,
            contentDescription = "${AMOUNT_STEP_ML}ml 늘리기",
            enabled = amountMl < range.last,
            onClick = { onAmountChange((amountMl + AMOUNT_STEP_ML).coerceIn(range)) }
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

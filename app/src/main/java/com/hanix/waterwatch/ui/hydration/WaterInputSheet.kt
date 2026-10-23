package com.hanix.waterwatch.ui.hydration

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanix.waterwatch.designsystem.Accent
import com.hanix.waterwatch.designsystem.BodyText
import com.hanix.waterwatch.designsystem.Spacing
import com.hanix.waterwatch.designsystem.SubtleSurface
import com.hanix.waterwatch.ui.component.AmountStepper
import com.hanix.waterwatch.ui.component.HydrationBottomSheet

// 크기·시간은 디자인 프로토타입 값 그대로. 색은 :designsystem 에서 가져온다.

private val STEPPER_PADDING_TOP = 28.dp
private val PRESETS_PADDING_TOP = 24.dp
private val PRESET_HEIGHT = 44.dp
private val PRESET_COLOR_TWEEN = tween<Color>(durationMillis = 200)

private val WATER_INPUT_RANGE_ML = 100..3_000
private const val DEFAULT_WATER_INPUT_ML = 300

/** 자주 마시는 양. 누르면 바로 그 값으로 바뀐다. */
private val PRESETS_ML = listOf(200, 300, 500)

/**
 * 마신 양을 직접 골라 기록하는 시트. 열 때마다 프로토타입처럼 300ml 에서 시작한다.
 * 고른 값은 "기록하기"를 눌러야 [onRecord] 로 나간다. 닫으면 버린다.
 */
@Composable
fun WaterInputSheet(onRecord: (ml: Int) -> Unit, onDismissRequest: () -> Unit) {
    var waterInputMl by rememberSaveable { mutableIntStateOf(DEFAULT_WATER_INPUT_ML) }

    HydrationBottomSheet(
        title = "마신 양 직접 입력",
        confirmText = "기록하기",
        onConfirm = { onRecord(waterInputMl) },
        onDismissRequest = onDismissRequest
    ) {
        AmountStepper(
            amountMl = waterInputMl,
            range = WATER_INPUT_RANGE_ML,
            onAmountChange = { waterInputMl = it },
            label = "마신 양",
            modifier = Modifier.padding(top = STEPPER_PADDING_TOP)
        )
        PresetButtons(
            selectedMl = waterInputMl,
            onSelect = { waterInputMl = it },
            modifier = Modifier.padding(top = PRESETS_PADDING_TOP)
        )
    }
}

/** 하나만 고를 수 있는 버튼 줄. 지금 값과 같은 버튼이 골라진 것으로 보인다. */
@Composable
private fun PresetButtons(selectedMl: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {
        PRESETS_ML.forEach { ml ->
            val selected = ml == selectedMl
            val container by animateColorAsState(if (selected) Accent else SubtleSurface, PRESET_COLOR_TWEEN, label = "presetContainer")
            val content by animateColorAsState(if (selected) Color.White else BodyText, PRESET_COLOR_TWEEN, label = "presetContent")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(PRESET_HEIGHT)
                    .clip(CircleShape)
                    .background(container)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(ml) }),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "%,dml".format(ml), color = content, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

package com.hanix.waterwatch.wear.ui.hydration

import android.view.ViewConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Dialog
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.hanix.waterwatch.designsystem.RollingNumber
import com.hanix.waterwatch.designsystem.SheetBackground
import com.hanix.waterwatch.designsystem.SheetButton
import com.hanix.waterwatch.designsystem.SheetContent
import com.hanix.waterwatch.designsystem.SheetPrimaryContainer
import com.hanix.waterwatch.designsystem.SheetPrimaryContent
import com.hanix.waterwatch.designsystem.SheetSubtleContent
import com.hanix.waterwatch.wear.R

// 크기는 디자인 프로토타입(갤럭시 워치 384px 보드)을 2배 해상도로 보고 절반으로 옮겼다.

private val SHEET_SPACING = 4.dp
private val PILL_HEIGHT = 48.dp
private val PILL_WIDTH = 112.dp
private val UNDO_PILL_WIDTH = 150.dp
private val BACK_WIDTH = 100.dp
private val BACK_ICON_SIZE = 14.dp
private val AMOUNT_ROW_WIDTH = 165.dp
private val STEP_BUTTON_SIZE = 48.dp
private val NO_CONTENT_PADDING = PaddingValues(0.dp)
private const val DISABLED_ALPHA = 0.4f

private val WATER_INPUT_STYLE = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)

private const val DEFAULT_WATER_INPUT_ML = 300
private const val WATER_INPUT_STEP_ML = 100
private const val MIN_WATER_INPUT_ML = 100
private const val MAX_WATER_INPUT_ML = 3_000

private enum class MorePage { Menu, WaterInput }

/**
 * 더보기 시트. 직접 입력한 양을 기록하거나 오늘 마지막 기록을 취소한다. 옆으로 밀면 닫힌다.
 * [canUndo] 가 false 면 취소 버튼을 흐리게 막는다.
 */
@Composable
fun MoreDialog(
    visible: Boolean,
    canUndo: Boolean,
    onRecord: (ml: Int) -> Unit,
    onUndo: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(visible = visible, onDismissRequest = onDismiss) {
        // 시트가 닫히면 이 내용이 사라졌다가 다시 만들어지므로, 여기 두면 열 때마다 메뉴부터 보인다.
        var page by remember { mutableStateOf(MorePage.Menu) }
        var waterInputMl by remember { mutableIntStateOf(DEFAULT_WATER_INPUT_ML) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SheetBackground),
            verticalArrangement = Arrangement.spacedBy(SHEET_SPACING, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (page) {
                MorePage.Menu -> MenuPage(
                    canUndo = canUndo,
                    // 폰 직접입력 시트처럼 열 때마다 기본값에서 시작한다.
                    onWaterInputClick = {
                        waterInputMl = DEFAULT_WATER_INPUT_ML
                        page = MorePage.WaterInput
                    },
                    onUndoClick = {
                        onUndo()
                        onDismiss()
                    },
                    onCloseClick = onDismiss
                )
                MorePage.WaterInput -> WaterInputPage(
                    ml = waterInputMl,
                    onMlChange = { waterInputMl = it.coerceIn(MIN_WATER_INPUT_ML, MAX_WATER_INPUT_ML) },
                    onBackClick = { page = MorePage.Menu },
                    onRecordClick = {
                        onRecord(waterInputMl)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun MenuPage(
    canUndo: Boolean,
    onWaterInputClick: () -> Unit,
    onUndoClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    SheetButton(text = "직접 입력", onClick = onWaterInputClick, width = PILL_WIDTH)
    SheetButton(text = "이전 기록 취소하기", onClick = onUndoClick, width = UNDO_PILL_WIDTH, enabled = canUndo)
    SheetButton(
        text = "닫기",
        onClick = onCloseClick,
        width = PILL_WIDTH,
        containerColor = Color.Transparent,
        contentColor = SheetSubtleContent,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun WaterInputPage(
    ml: Int,
    onMlChange: (Int) -> Unit,
    onBackClick: () -> Unit,
    onRecordClick: () -> Unit
) {
    BackButton(onClick = onBackClick)
    Row(
        modifier = Modifier
            .width(AMOUNT_ROW_WIDTH)
            .crownSteps { steps -> onMlChange(ml + steps * WATER_INPUT_STEP_ML) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SheetButton(
            text = "−",
            onClick = { onMlChange(ml - WATER_INPUT_STEP_ML) },
            width = STEP_BUTTON_SIZE,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RollingNumber(value = ml, style = WATER_INPUT_STYLE, color = SheetContent)
            Text(text = "ml", color = SheetContent, fontSize = 12.sp)
        }
        SheetButton(
            text = "+",
            onClick = { onMlChange(ml + WATER_INPUT_STEP_ML) },
            width = STEP_BUTTON_SIZE,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
    SheetButton(
        text = "기록하기",
        onClick = onRecordClick,
        width = PILL_WIDTH,
        containerColor = SheetPrimaryContainer,
        contentColor = SheetPrimaryContent,
        fontWeight = FontWeight.ExtraBold
    )
}

/**
 * 크라운·베젤을 한 칸 돌릴 때마다 칸 수를 넘긴다. 시계 방향이 + 다.
 * 한 칸의 크기는 기기가 정한 스크롤 한 단위(scaledVerticalScrollFactor)를 쓴다. 베젤은 딸깍 한 번이 한 단위다.
 * 회전 입력은 포커스를 가진 요소로만 오므로 화면에 나오면 바로 포커스를 가져온다.
 */
@Composable
private fun Modifier.crownSteps(onSteps: (Int) -> Unit): Modifier {
    val context = LocalContext.current
    val pxPerStep = remember(context) { ViewConfiguration.get(context).scaledVerticalScrollFactor }
    val focusRequester = remember { FocusRequester() }
    var pendingPx by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
    return onRotaryScrollEvent { event ->
        pendingPx += event.verticalScrollPixels
        val steps = (pendingPx / pxPerStep).toInt()
        if (steps != 0) {
            pendingPx -= steps * pxPerStep
            onSteps(steps)
        }
        true
    }
        .focusRequester(focusRequester)
        .focusable()
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(BACK_WIDTH, PILL_HEIGHT),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = SheetContent),
        contentPadding = NO_CONTENT_PADDING
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = null,
                tint = SheetSubtleContent,
                modifier = Modifier.size(BACK_ICON_SIZE)
            )
            Text(text = "마신 양", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** 시트 안의 알약·원형 버튼. 막히면 프로토타입처럼 전체를 흐리게 한다. */
@Composable
private fun SheetButton(
    text: String,
    onClick: () -> Unit,
    width: Dp,
    enabled: Boolean = true,
    containerColor: Color = SheetButton,
    contentColor: Color = SheetContent,
    fontSize: TextUnit = 15.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(width, PILL_HEIGHT),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = containerColor.alpha * DISABLED_ALPHA),
            disabledContentColor = contentColor.copy(alpha = contentColor.alpha * DISABLED_ALPHA)
        ),
        contentPadding = NO_CONTENT_PADDING
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = 1
        )
    }
}

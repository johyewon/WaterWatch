package com.hanix.waterwatch.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanix.waterwatch.designsystem.BottomSheetBackground
import com.hanix.waterwatch.designsystem.BottomSheetHandle
import com.hanix.waterwatch.designsystem.BottomSheetScrim
import com.hanix.waterwatch.designsystem.Radius
import com.hanix.waterwatch.designsystem.Spacing
import com.hanix.waterwatch.designsystem.TitleText
import kotlinx.coroutines.launch

// 크기는 디자인 프로토타입 값 그대로. 색·모서리는 :designsystem 에서 가져온다.

private val CONTENT_PADDING_HORIZONTAL = 20.dp
private val CONTENT_PADDING_BOTTOM = 30.dp
private val ACTIONS_PADDING_TOP = 32.dp
private val HANDLE_PADDING_TOP = 12.dp
private val HANDLE_WIDTH = 36.dp
private val HANDLE_HEIGHT = 5.dp
private val TITLE_PADDING_TOP = 20.dp

/** 취소 : 확인 버튼 너비 비율 */
private const val CANCEL_WEIGHT = 1f
private const val CONFIRM_WEIGHT = 1.6f

/**
 * 제목, [content], 취소·확인 버튼으로 된 바텀시트. 목표 설정과 직접 입력이 같이 쓴다.
 * 화면에 띄울지는 호출하는 쪽이 정하므로 [onDismissRequest]·[onConfirm] 에서 시트를 내려야 한다.
 * 버튼으로 닫을 때도 끌어내릴 때처럼 내려가는 애니메이션이 끝난 뒤에 알린다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HydrationBottomSheet(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    /*
     * 처음 고른 닫기 동작(취소·확인·바깥 탭) 하나만, 시트가 다 내려간 뒤 한 번 알린다.
     * 내려가는 동안 버튼을 또 누르거나 바깥이 눌리면 시트가 스스로 닫히며 우리 애니메이션을 끊는데,
     * 그때도 처음 고른 동작이 실행되도록 기억해 둔다. 받는 쪽이 값을 저장하거나 화면을 넘기므로 두 번 일어나면 안 된다.
     */
    var chosenAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var notified by remember { mutableStateOf(false) }
    val notifyOnce: (() -> Unit) -> Unit = { action ->
        if (notified.not()) {
            notified = true
            action()
        }
    }
    val hideThen: (() -> Unit) -> Unit = { action ->
        if (chosenAction == null) {
            chosenAction = action
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (sheetState.isVisible.not()) notifyOnce(action)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { notifyOnce(chosenAction ?: onDismissRequest) },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = Radius.XLarge, topEnd = Radius.XLarge),
        containerColor = BottomSheetBackground,
        scrimColor = BottomSheetScrim,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = HANDLE_PADDING_TOP)
                    .size(HANDLE_WIDTH, HANDLE_HEIGHT)
                    .background(BottomSheetHandle, CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = CONTENT_PADDING_HORIZONTAL)
                .padding(bottom = CONTENT_PADDING_BOTTOM)
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TITLE_PADDING_TOP),
                color = TitleText,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            content()
            Row(
                modifier = Modifier.padding(top = ACTIONS_PADDING_TOP),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                HydrationButton(
                    text = "취소",
                    onClick = { hideThen(onDismissRequest) },
                    style = HydrationButtonStyle.Secondary,
                    modifier = Modifier.weight(CANCEL_WEIGHT)
                )
                HydrationButton(
                    text = confirmText,
                    onClick = { hideThen(onConfirm) },
                    style = HydrationButtonStyle.Primary,
                    modifier = Modifier.weight(CONFIRM_WEIGHT)
                )
            }
        }
    }
}

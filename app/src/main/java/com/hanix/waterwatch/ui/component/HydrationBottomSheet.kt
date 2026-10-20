package com.hanix.waterwatch.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanix.waterwatch.designsystem.BottomSheetBackground
import com.hanix.waterwatch.designsystem.BottomSheetHandle
import com.hanix.waterwatch.designsystem.BottomSheetScrim
import com.hanix.waterwatch.designsystem.BottomSheetTitle
import com.hanix.waterwatch.designsystem.Radius
import com.hanix.waterwatch.designsystem.Spacing
import kotlinx.coroutines.launch

// 크기는 디자인 프로토타입 값 그대로. 색·모서리는 :designsystem 에서 가져온다.

private val CONTENT_PADDING_HORIZONTAL = 20.dp
private val CONTENT_PADDING_BOTTOM = 30.dp
private val ACTIONS_PADDING_TOP = 32.dp
private val HANDLE_WIDTH = 36.dp
private val HANDLE_HEIGHT = 5.dp

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
    val hideThen: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (sheetState.isVisible.not()) action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = Radius.XLarge, topEnd = Radius.XLarge),
        containerColor = BottomSheetBackground,
        scrimColor = BottomSheetScrim,
        // ponytail: 공식 핸들은 위아래 여백이 22dp 로 고정이라 프로토타입(위 12, 제목까지 20)보다 조금 넓다. 맞춰야 하면 핸들을 직접 그린다.
        dragHandle = {
            BottomSheetDefaults.DragHandle(width = HANDLE_WIDTH, height = HANDLE_HEIGHT, color = BottomSheetHandle)
        }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = CONTENT_PADDING_HORIZONTAL)
                .padding(bottom = CONTENT_PADDING_BOTTOM)
        ) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                color = BottomSheetTitle,
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

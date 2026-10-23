package com.hanix.waterwatch.wear.ui.hydration

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.hanix.waterwatch.designsystem.Accent
import com.hanix.waterwatch.designsystem.Ink
import com.hanix.waterwatch.designsystem.R
import com.hanix.waterwatch.designsystem.ToastBackground

// 크기·시간은 디자인 프로토타입(갤럭시 워치 384px 보드)을 2배 해상도로 보고 절반으로 옮겼다.

private val TOAST_MIN_HEIGHT = 32.dp
// 보드는 양옆 52px 고정(너비 140dp). 글자가 그보다 길어도 잘리지 않도록 고정 대신 최소 너비로 둔다.
private val TOAST_MIN_WIDTH = 140.dp
private val ICON_SIZE = 12.dp

/** 위에서 이만큼 내려오며 나타난다. */
private val TOAST_DROP = 4.dp
private const val TOAST_ENTER_MILLIS = 250
private val TOAST_EASING = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/**
 * 기록·취소 요청을 보낸 직후 잠깐 띄우는 안내.
 * 처음 나타날 때만 내려오고, 떠 있는 동안 [message] 가 바뀌면 글자만 바뀐다.
 */
@Composable
fun WearToast(message: String, modifier: Modifier = Modifier) {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(entrance) {
        entrance.animateTo(1f, tween(TOAST_ENTER_MILLIS, easing = TOAST_EASING))
    }

    Row(
        modifier = modifier
            .graphicsLayer {
                alpha = entrance.value
                translationY = -TOAST_DROP.toPx() * (1 - entrance.value)
            }
            .defaultMinSize(minWidth = TOAST_MIN_WIDTH, minHeight = TOAST_MIN_HEIGHT)
            .background(ToastBackground, CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            // 화면을 보지 않는 사용자에게도 결과를 읽어준다.
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_toast_check),
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(ICON_SIZE)
        )
        Text(text = message, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

package com.hanix.waterwatch.ui.hydration

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.hanix.waterwatch.designsystem.Accent
import com.hanix.waterwatch.designsystem.Ink
import com.hanix.waterwatch.designsystem.R
import com.hanix.waterwatch.designsystem.Spacing
import com.hanix.waterwatch.designsystem.ToastBackground
import com.hanix.waterwatch.designsystem.ToastTextStyle

// 크기·시간은 디자인 프로토타입 값 그대로. 색·글자·모서리·간격은 테마에서 가져온다.

private val TOAST_MIN_HEIGHT = 52.dp

/** 아래에서 이만큼 올라오며 나타난다. */
private val TOAST_RISE = 12.dp
private const val TOAST_ENTER_MILLIS = 280
private val TOAST_EASING = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/**
 * 기록·목표 변경 직후 잠깐 띄우는 안내.
 * 처음 나타날 때만 올라오고, 떠 있는 동안 [message] 가 바뀌면 글자만 바뀐다. 사라질 때는 바로 사라진다.
 */
@Composable
fun HydrationToast(message: String, modifier: Modifier = Modifier) {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(entrance) {
        entrance.animateTo(1f, tween(TOAST_ENTER_MILLIS, easing = TOAST_EASING))
    }

    Row(
        modifier = modifier
            .graphicsLayer {
                alpha = entrance.value
                translationY = TOAST_RISE.toPx() * (1 - entrance.value)
            }
            .fillMaxWidth()
            .defaultMinSize(minHeight = TOAST_MIN_HEIGHT)
            .background(ToastBackground, MaterialTheme.shapes.medium)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            // 화면을 보지 않는 사용자에게도 결과를 읽어준다.
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(R.drawable.ic_toast_check), contentDescription = null, tint = Accent)
        Text(text = message, color = Ink, style = ToastTextStyle)
    }
}

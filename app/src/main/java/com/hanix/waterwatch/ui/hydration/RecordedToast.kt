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
import com.hanix.waterwatch.R
import com.hanix.waterwatch.ui.theme.Accent
import com.hanix.waterwatch.ui.theme.Ink
import com.hanix.waterwatch.ui.theme.ToastBackground
import com.hanix.waterwatch.ui.theme.ToastTextStyle
import kotlin.math.roundToInt

// 크기·시간은 디자인 프로토타입 값 그대로. 색·글자·모서리는 테마에서 가져온다.

private val TOAST_MIN_HEIGHT = 52.dp

/** 아래에서 이만큼 올라오며 나타난다. */
private val TOAST_RISE = 12.dp
private const val TOAST_ENTER_MILLIS = 280
private val TOAST_EASING = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/**
 * 기록 직후 잠깐 띄우는 "OOml 기록했어요" 안내.
 * 처음 나타날 때만 올라오고, 떠 있는 동안 [ml] 이 바뀌면 글자만 바뀐다. 사라질 때는 바로 사라진다.
 */
@Composable
fun RecordedToast(ml: Double, modifier: Modifier = Modifier) {
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
            // 프로토타입 모서리 12dp 는 Material3 기본 medium 과 같다.
            .background(ToastBackground, MaterialTheme.shapes.medium)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            // 화면을 보지 않는 사용자에게도 기록 결과를 읽어준다.
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(R.drawable.ic_toast_check), contentDescription = null, tint = Accent)
        Text(text = "${ml.roundToInt()}ml 기록했어요", color = Ink, style = ToastTextStyle)
    }
}

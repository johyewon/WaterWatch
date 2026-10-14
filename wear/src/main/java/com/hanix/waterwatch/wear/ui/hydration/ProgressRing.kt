package com.hanix.waterwatch.wear.ui.hydration

import androidx.compose.animation.core.Ease
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hanix.waterwatch.designsystem.Ink

// 값은 디자인 프로토타입(갤럭시 워치 384px 보드)에서 옮겼다. 보드 값의 절반을 dp 로 쓴다.

// 화면 테두리에서 링 선 가운데까지
private val RING_INSET = 5.dp
private val TRACK_WIDTH = 5.dp
private const val TRACK_ALPHA = 0.22f

// 흰 진행 선 아래에 조금 더 굵은 어두운 선을 깔아 밝은 배경에서도 보이게 한다.
private val ARC_OUTLINE_WIDTH = 5.dp
private const val ARC_OUTLINE_ALPHA = 0.35f
private val ARC_WIDTH = 3.dp

private const val SWEEP_MILLIS = 800
private const val START_ANGLE = -90f

/**
 * 화면 테두리를 따라 12시부터 시계 방향으로 목표 진행률([progress], 0..1)만큼 채워지는 링.
 *
 * Wear Material3 CircularProgressIndicator 는 선 굵기가 하나뿐이라 진행 선 아래 어두운 테두리를 깔 수 없어서 직접 그린다.
 */
@Composable
fun ProgressRing(progress: Float, modifier: Modifier = Modifier) {
    val sweep by animateFloatAsState(progress, tween(SWEEP_MILLIS, easing = Ease), label = "ringSweep")

    Spacer(
        modifier = modifier.drawBehind {
            drawCircle(
                color = Ink.copy(alpha = TRACK_ALPHA),
                radius = size.minDimension / 2 - RING_INSET.toPx(),
                style = Stroke(TRACK_WIDTH.toPx())
            )
            if (sweep > 0f) {
                drawProgressArc(sweep, Ink.copy(alpha = ARC_OUTLINE_ALPHA), ARC_OUTLINE_WIDTH)
                drawProgressArc(sweep, Color.White, ARC_WIDTH)
            }
        }
    )
}

private fun DrawScope.drawProgressArc(sweep: Float, color: Color, width: Dp) {
    val inset = RING_INSET.toPx()
    val diameter = size.minDimension - inset * 2
    drawArc(
        color = color,
        startAngle = START_ANGLE,
        sweepAngle = 360f * sweep,
        useCenter = false,
        topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2),
        size = Size(diameter, diameter),
        style = Stroke(width.toPx(), cap = StrokeCap.Round)
    )
}

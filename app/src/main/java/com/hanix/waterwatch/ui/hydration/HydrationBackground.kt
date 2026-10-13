package com.hanix.waterwatch.ui.hydration

import androidx.compose.animation.core.Ease
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.hanix.waterwatch.R
import com.hanix.waterwatch.ui.theme.BackgroundDeepBottom
import com.hanix.waterwatch.ui.theme.BackgroundDeepMiddle
import com.hanix.waterwatch.ui.theme.BackgroundDeepTop
import com.hanix.waterwatch.ui.theme.BackgroundLightBottom
import com.hanix.waterwatch.ui.theme.BackgroundLightMiddle
import com.hanix.waterwatch.ui.theme.BackgroundLightTop
import com.hanix.waterwatch.ui.theme.WaterEmptyBottom
import com.hanix.waterwatch.ui.theme.WaterEmptyMiddle
import com.hanix.waterwatch.ui.theme.WaterEmptyTop
import com.hanix.waterwatch.ui.theme.WaterFullBottom
import com.hanix.waterwatch.ui.theme.WaterFullMiddle
import com.hanix.waterwatch.ui.theme.WaterFullTop
import kotlin.math.roundToInt

// 크기·시간은 디자인 프로토타입(390pt 폭 기준) 값 그대로. pt 는 dp 로 옮긴다. 색은 ui.theme 에 모아 뒀다.

private val LIGHT_GRADIENT = Brush.verticalGradient(
    0f to BackgroundLightTop,
    0.55f to BackgroundLightMiddle,
    1f to BackgroundLightBottom
)

private val DEEP_GRADIENT = Brush.verticalGradient(
    0f to BackgroundDeepTop,
    0.55f to BackgroundDeepMiddle,
    1f to BackgroundDeepBottom
)

private const val DEEPEN_MILLIS = 1_400

/** 물은 화면을 다 채우지 않고 이만큼에서 멈춘다. */
private const val MAX_WATER_LEVEL = 0.97f

/** 차오를 때 살짝 지나쳤다가 자리 잡는다. 프로토타입 스프링 k=12, c=4.6 → 감쇠비 약 0.66 */
private val WATER_LEVEL_SPRING = spring<Float>(dampingRatio = 0.66f, stiffness = 12f)

/** 진행률 0 → 1 에 따라 물 위·가운데·아래 색이 이 쌍 사이에서 바뀐다. */
private val WATER_COLORS = listOf(
    WaterEmptyTop to WaterFullTop,
    WaterEmptyMiddle to WaterFullMiddle,
    WaterEmptyBottom to WaterFullBottom
)
private const val WATER_COLOR_MILLIS = 1_200

/**
 * 수면 경계를 흐리게 하는 정도. 프로토타입 SVG stdDeviation 30 을 블러 반지름으로 옮긴 값이다.
 * 확인 필요: RenderEffect 는 반지름을 sigma = 0.577 * r + 0.5 로 바꾸는 것으로 알고 있어서 그 식으로 계산했다.
 * 실기기에서 프로토타입보다 경계가 덜 흐리거나 더 흐리면 이 값을 조정한다. Android 12 미만에서는 블러가 적용되지 않는다.
 */
private val WATER_EDGE_BLUR = 51.dp

/** 물 그라데이션은 화면 아래로 이만큼 더 이어진 영역 기준으로 그린다(프로토타입 path 범위). */
private val WATER_GRADIENT_OVERFLOW = 60.dp

private val GRAIN_TILE = 160.dp
private val GRAIN_DRIFT = Offset(160f, 80f)
private const val GRAIN_DRIFT_MILLIS = 30_000
private const val GRAIN_ALPHA = 0.6f

/**
 * 목표 진행률([progress], 0..1)에 따라 배경이 짙어지고 아래에서 물이 차오른다.
 * 맨 위에는 천천히 흐르는 질감(grain)을 겹친다.
 */
@Composable
fun HydrationBackground(progress: Float, modifier: Modifier = Modifier) {
    val deepAlpha by animateFloatAsState(progress, tween(DEEPEN_MILLIS, easing = Ease), label = "deepAlpha")
    val waterLevel by animateFloatAsState(progress * MAX_WATER_LEVEL, WATER_LEVEL_SPRING, label = "waterLevel")
    val waterColor by animateFloatAsState(progress, tween(WATER_COLOR_MILLIS, easing = Ease), label = "waterColor")

    Box(modifier = modifier) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(LIGHT_GRADIENT)
                    drawRect(DEEP_GRADIENT, alpha = deepAlpha)
                }
        )
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .blur(WATER_EDGE_BLUR)
                .drawBehind {
                    val surfaceY = size.height * (1 - waterLevel)
                    val (top, middle, bottom) = WATER_COLORS.map { (empty, full) -> mixSrgb(empty, full, waterColor) }
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to top,
                            0.45f to middle,
                            1f to bottom,
                            startY = surfaceY,
                            endY = size.height + WATER_GRADIENT_OVERFLOW.toPx()
                        ),
                        topLeft = Offset(0f, surfaceY)
                    )
                }
        )
        Grain(modifier = Modifier.fillMaxSize())
    }
}

/**
 * 질감 이미지를 타일로 깔고 대각선으로 천천히 움직인다. 아래 층과는 soft-light 로 섞는다.
 *
 * 이 Spacer 에 graphicsLayer 를 달면 따로 그린 다음 합치기 때문에 soft-light 가 아래 층과 섞이지 않는다.
 */
@Composable
private fun Grain(modifier: Modifier = Modifier) {
    val grain = ImageBitmap.imageResource(R.drawable.grain)
    val drift by rememberInfiniteTransition(label = "grain").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(GRAIN_DRIFT_MILLIS, easing = LinearEasing)),
        label = "grainDrift"
    )

    Spacer(
        modifier = modifier.drawBehind {
            val tile = GRAIN_TILE.roundToPx()
            val shiftX = (GRAIN_DRIFT.x.dp.toPx() * drift).roundToInt() % tile
            val shiftY = (GRAIN_DRIFT.y.dp.toPx() * drift).roundToInt() % tile
            for (x in shiftX - tile until size.width.roundToInt() step tile) {
                for (y in shiftY - tile until size.height.roundToInt() step tile) {
                    drawImage(
                        image = grain,
                        dstOffset = IntOffset(x, y),
                        dstSize = IntSize(tile, tile),
                        alpha = GRAIN_ALPHA,
                        blendMode = BlendMode.Softlight
                    )
                }
            }
        }
    )
}

/** Compose 의 Color lerp 는 Oklab 에서 섞어서, 프로토타입처럼 sRGB 채널을 그대로 섞는다. */
private fun mixSrgb(start: Color, stop: Color, fraction: Float): Color = Color(
    red = lerp(start.red, stop.red, fraction),
    green = lerp(start.green, stop.green, fraction),
    blue = lerp(start.blue, stop.blue, fraction)
)

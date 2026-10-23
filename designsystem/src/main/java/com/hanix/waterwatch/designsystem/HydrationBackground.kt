package com.hanix.waterwatch.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Ease
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.pow
import kotlin.math.roundToInt

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

/** 차오를 때 살짝 지나쳤다가 자리 잡는다. 프로토타입 스프링 k=12, c=4.6 → 감쇠비 약 0.66 */
private val WATER_LEVEL_SPRING = spring<Float>(dampingRatio = 0.66f, stiffness = 12f)

/** 진행률 0 → 1 에 따라 물 위·가운데·아래 색이 이 쌍 사이에서 바뀐다. */
private val WATER_COLORS = listOf(
    WaterEmptyTop to WaterFullTop,
    WaterEmptyMiddle to WaterFullMiddle,
    WaterEmptyBottom to WaterFullBottom
)
private const val WATER_COLOR_MILLIS = 1_200

private const val GRAIN_DRIFT_MILLIS = 30_000
private const val GRAIN_ALPHA = 0.6f

/** 폰 프로토타입은 진행률이 이만큼부터 아래쪽 글자 뒤가 물로 덮인다고 본다. */
private const val FOOTER_WATER_PROGRESS = 0.08f

/** 폰·워치 프로토타입 보드마다 다른 배경 값 */
class HydrationBackgroundSpec(
    /** 물은 화면을 다 채우지 않고 이만큼에서 멈춘다. */
    val maxWaterLevel: Float,
    /** 물이 이 높이(아래에서부터 화면 비율)를 넘으면 위쪽 글자 뒤가 물로 덮인다. */
    val headerWaterLevel: Float,
    val deepenMillis: Int,
    /** 짙은 배경의 투명도는 진행률의 이 거듭제곱이다. */
    val deepExponent: Float,
    /**
     * 수면 경계를 흐리게 하는 정도. 프로토타입 SVG stdDeviation 을 블러 반지름으로 옮긴 값이다.
     * 확인 필요: RenderEffect 는 반지름을 sigma = 0.577 * r + 0.5 로 바꾸는 것으로 알고 있어서 그 식으로 계산했다.
     * 실기기에서 프로토타입보다 경계가 덜 흐리거나 더 흐리면 이 값을 조정한다. Android 12 미만에서는 블러가 적용되지 않는다.
     */
    val waterEdgeBlur: Dp,
    /** 물 그라데이션은 화면 아래로 이만큼 더 이어진 영역 기준으로 그린다(프로토타입 path 범위). */
    val waterGradientOverflow: Dp,
    val grainTile: Dp,
    /** 질감이 30초 동안 흘러가는 거리 */
    val grainDrift: DpOffset
) {

    companion object {

        /** 390pt 폭 보드. pt 를 dp 로 그대로 옮겼다. */
        val Phone = HydrationBackgroundSpec(
            maxWaterLevel = 0.97f,
            headerWaterLevel = 0.72f,
            deepenMillis = 1_400,
            deepExponent = 1f,
            waterEdgeBlur = 51.dp,
            waterGradientOverflow = 60.dp,
            grainTile = 160.dp,
            grainDrift = DpOffset(160.dp, 80.dp)
        )

        /** 갤럭시 워치 384px 보드. 2배 해상도로 보고 절반을 dp 로 옮겼다. */
        val Watch = HydrationBackgroundSpec(
            maxWaterLevel = 0.8f,
            headerWaterLevel = 0.62f,
            deepenMillis = 1_200,
            deepExponent = 0.8f,
            waterEdgeBlur = 25.dp,
            waterGradientOverflow = 30.dp,
            grainTile = 60.dp,
            grainDrift = DpOffset(60.dp, 30.dp)
        )
    }
}

/**
 * 목표 진행률([progress], 0..1)에 따라 배경이 짙어지고 아래에서 물이 차오른다.
 * 맨 위에는 천천히 흐르는 질감(grain)을 겹친다. [animated] 가 false 면 질감이 그 자리에 멈춘다.
 */
@Composable
fun HydrationBackground(
    progress: Float,
    spec: HydrationBackgroundSpec,
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val deepAlpha by animateFloatAsState(
        targetValue = progress.pow(spec.deepExponent),
        animationSpec = tween(spec.deepenMillis, easing = Ease),
        label = "deepAlpha"
    )
    val waterLevel by animateFloatAsState(progress * spec.maxWaterLevel, WATER_LEVEL_SPRING, label = "waterLevel")
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
                .blur(spec.waterEdgeBlur)
                .drawBehind {
                    val surfaceY = size.height * (1 - waterLevel)
                    val (top, middle, bottom) = WATER_COLORS.map { (empty, full) -> mixSrgb(empty, full, waterColor) }
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to top,
                            0.45f to middle,
                            1f to bottom,
                            startY = surfaceY,
                            endY = size.height + spec.waterGradientOverflow.toPx()
                        ),
                        topLeft = Offset(0f, surfaceY)
                    )
                }
        )
        Grain(tileSize = spec.grainTile, drift = spec.grainDrift, animated = animated, modifier = Modifier.fillMaxSize())
    }
}

/**
 * 질감 이미지를 타일로 깔고 대각선으로 천천히 움직인다. 아래 층과는 soft-light 로 섞는다.
 *
 * 이 Spacer 에 graphicsLayer 를 달면 따로 그린 다음 합치기 때문에 soft-light 가 아래 층과 섞이지 않는다.
 */
@Composable
private fun Grain(tileSize: Dp, drift: DpOffset, animated: Boolean, modifier: Modifier = Modifier) {
    val grain = ImageBitmap.imageResource(R.drawable.grain)
    val driftProgress = remember { Animatable(0f) }
    // 반복은 시작한 값에서 다시 시작하므로, 멈췄다 다시 움직일 때는 처음부터 흘린다.
    LaunchedEffect(animated) {
        if (animated) {
            driftProgress.snapTo(0f)
            driftProgress.animateTo(1f, infiniteRepeatable(tween(GRAIN_DRIFT_MILLIS, easing = LinearEasing)))
        }
    }

    Spacer(
        modifier = modifier.drawBehind {
            val tile = tileSize.roundToPx()
            val shiftX = (drift.x.toPx() * driftProgress.value).roundToInt() % tile
            val shiftY = (drift.y.toPx() * driftProgress.value).roundToInt() % tile
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

/**
 * 진행률이 [progress] 일 때 화면 위쪽 글자 뒤의 배경색. 글자색을 고르는 데 쓴다.
 * 물이 [HydrationBackgroundSpec.headerWaterLevel] 을 넘으면 물 윗면 색, 아니면 배경 위쪽 색이다. 프로토타입 계산 그대로.
 */
fun HydrationBackgroundSpec.headerBackgroundColor(progress: Float): Color =
    if (progress * maxWaterLevel > headerWaterLevel) {
        mixSrgb(WaterEmptyTop, WaterFullTop, progress)
    } else {
        mixSrgb(BackgroundLightTop, BackgroundDeepTop, progress.pow(deepExponent))
    }

/** 폰 화면 아래쪽 글자 뒤의 배경색. 프로토타입 계산 그대로. */
fun footerBackgroundColor(progress: Float): Color =
    if (progress >= FOOTER_WATER_PROGRESS) {
        mixSrgb(WaterEmptyBottom, WaterFullBottom, progress)
    } else {
        mixSrgb(BackgroundLightBottom, BackgroundDeepBottom, progress)
    }

/** [background] 위에서 [darkText] 보다 흰 글자의 대비가 같거나 크면 흰색을 준다. 프로토타입 inkFor 와 같은 WCAG 대비 비교다. */
fun readableTextColor(background: Color, darkText: Color): Color {
    val backgroundLuminance = background.luminance()
    val whiteContrast = 1.05f / (backgroundLuminance + 0.05f)
    val darkContrast = (backgroundLuminance + 0.05f) / (darkText.luminance() + 0.05f)
    return if (whiteContrast >= darkContrast) Color.White else darkText
}

/** Compose 의 Color lerp 는 Oklab 에서 섞어서, 프로토타입처럼 sRGB 채널을 그대로 섞는다. */
private fun mixSrgb(start: Color, stop: Color, fraction: Float): Color = Color(
    red = lerp(start.red, stop.red, fraction),
    green = lerp(start.green, stop.green, fraction),
    blue = lerp(start.blue, stop.blue, fraction)
)

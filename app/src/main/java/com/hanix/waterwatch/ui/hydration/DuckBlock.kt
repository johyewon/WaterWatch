package com.hanix.waterwatch.ui.hydration

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.hanix.waterwatch.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/** 오리 이미지 비율 그대로. 프로토타입 폰 화면(390pt 폭)에서 쓴 크기 */
private val DUCK_WIDTH = 132.dp
private val DUCK_HEIGHT = 149.dp

// 아래 속도 값들은 디자인 프로토타입(390pt 폭 기준)에서 옮긴 것이라 dp/s 단위다.

/** 놓으면 가운데로 돌아오면서 살짝 지나쳤다 온다. 프로토타입 스프링 k=34, c=2.8 → 감쇠비 약 0.24 */
private val HOME_SPRING = spring<Float>(dampingRatio = 0.24f, stiffness = 34f)

/** 벽에 부딪히면 이 비율만큼 속도를 남기고 반대로 튕긴다. */
private const val WALL_RESTITUTION = 0.62f

private const val SQUASH_MIN_SPEED = 60f
private const val SQUASH_PER_SPEED = 1f / 2600
private const val MAX_SQUASH = 0.22f

/** 찌그러진 축의 줄어든 만큼에 이 비율을 곱해서 다른 축을 늘린다. */
private const val SQUASH_BULGE = 0.6f

private val SQUASH_RECOVER = spring<Float>(stiffness = Spring.StiffnessLow)

/** 눌렀을 때 움찔하는 크기 */
private const val POKE_SCALE_X = 0.9f
private const val POKE_SCALE_Y = 0.86f

/** 누른 지점 반대쪽으로 밀리는 속도 */
private const val POKE_SPEED = 90f

private const val MAX_FLING_SPEED = 1800f

private const val ROTATION_DEG_PER_SPEED = 0.04f
private const val MAX_ROTATION_DEG = 22f

/** 손을 뗀 뒤 이만큼 더 X 눈을 하고 있다가 돌아온다. */
private const val SQUEEZED_FACE_HOLD_MILLIS = 700L

/** 연타하면 앞 소리가 끝나기 전에 겹쳐 난다. */
private const val QUACK_MAX_STREAMS = 3

/** 진행률이 이 이상이면 물에 뜬 것처럼 까딱인다. 프로토타입은 0.5, 정확한 값을 확인하기 전까지 0.6 으로 둔다. */
private const val FLOATING_PROGRESS = 0.6f

/** 까딱임 한 방향(내려와 있다가 올라가기까지) 시간. 왕복 3.2초 */
private const val BOB_HALF_MILLIS = 1_600
private val BOB_RISE = 5.dp
private const val BOB_TILT_DEG = -1.5f

/** 선글라스는 위에서 기울어진 채 떨어지며 살짝 지나쳤다가 제자리에 앉는다. */
private const val SUNGLASSES_ENTER_MILLIS = 700
private val SUNGLASSES_EASING = CubicBezierEasing(0.3f, 1.5f, 0.5f, 1f)
private const val SUNGLASSES_DROP_FRACTION = 0.4f
private const val SUNGLASSES_TILT_DEG = -12f

/** 전체 시간 중 이 지점까지 선글라스가 서서히 나타난다. */
private const val SUNGLASSES_FADE_IN_END = 0.6f

/**
 * 오리 캐릭터. 누르면 꽥 소리와 함께 X 눈으로 움찔하고, 끌어다 던지면 벽에 튕기다가 가운데로 돌아온다.
 * 목표 진행률([progress], 0..1)이 오르면 물에 떠서 까딱이고, 다 채우면 선글라스를 쓴다.
 */
@Composable
fun DuckBlock(progress: Float, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val quack = rememberQuack()
    val density = LocalDensity.current
    val x = remember { Animatable(0f) }
    val y = remember { Animatable(0f) }
    val squashX = remember { Animatable(1f) }
    val squashY = remember { Animatable(1f) }
    // 찌그러짐의 기준점. 마지막으로 누른 지점에서 찌그러진다.
    var pivot by remember { mutableStateOf(TransformOrigin.Center) }
    var squeezed by remember { mutableStateOf(false) }
    var relaxFace: Job? by remember { mutableStateOf(null) }

    fun squash(scaleX: Float, scaleY: Float) {
        scope.launch {
            squashX.snapTo(scaleX)
            squashX.animateTo(1f, SQUASH_RECOVER)
        }
        scope.launch {
            squashY.snapTo(scaleY)
            squashY.animateTo(1f, SQUASH_RECOVER)
        }
    }

    /** 부딪힌 축은 눌리고 다른 축은 부푼다. */
    fun squashOnWallHit(speedPx: Float, hitX: Boolean) {
        val speed = with(density) { speedPx.toDp().value }
        if (speed <= SQUASH_MIN_SPEED) return
        val pressed = 1 - min(MAX_SQUASH, speed * SQUASH_PER_SPEED)
        val bulged = 1 + (1 - pressed) * SQUASH_BULGE
        if (hitX) squash(pressed, bulged) else squash(bulged, pressed)
    }

    BoxWithConstraints(modifier = modifier) {
        // 영역이 오리보다 작으면 음수가 되어 coerceIn·updateBounds 가 예외를 던지므로 0 에서 막는다.
        val rangeX = ((constraints.maxWidth - with(density) { DUCK_WIDTH.toPx() }) / 2).coerceAtLeast(0f)
        val rangeY = ((constraints.maxHeight - with(density) { DUCK_HEIGHT.toPx() }) / 2).coerceAtLeast(0f)

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(x.value.roundToInt(), y.value.roundToInt()) }
                .size(DUCK_WIDTH, DUCK_HEIGHT)
                // 찌그러짐·회전은 그리기에만 적용되도록 터치 처리 뒤에 둔다.
                .pointerInput(rangeX, rangeY) {
                    val tracker = VelocityTracker()
                    val maxFling = MAX_FLING_SPEED.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        relaxFace?.cancel()
                        squeezed = true
                        quack()
                        scope.launch {
                            x.stop()
                            y.stop()
                        }
                        pivot = TransformOrigin(down.position.x / size.width, down.position.y / size.height)
                        squash(POKE_SCALE_X, POKE_SCALE_Y)
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val poke = (center - down.position).let {
                            Velocity(it.x / center.x, it.y / center.y) * POKE_SPEED.dp.toPx()
                        }

                        tracker.resetTracking()
                        // 오리는 벽에서 멈춰도 던지는 속도는 손가락 기준이어야 해서 벽에 막히기 전 위치를 따로 쌓는다.
                        val start = Offset(x.value, y.value)
                        var finger = start
                        tracker.addPosition(down.uptimeMillis, finger)
                        drag(down.id) { change ->
                            finger += change.positionChange()
                            scope.launch {
                                x.snapTo(finger.x.coerceIn(-rangeX, rangeX))
                                y.snapTo(finger.y.coerceIn(-rangeY, rangeY))
                            }
                            tracker.addPosition(change.uptimeMillis, finger)
                            change.consume()
                        }

                        // 탭이면 누른 반대쪽으로 살짝 밀리고, 끌었으면 손가락 속도만 쓴다.
                        val dragged = (finger - start).getDistance() > viewConfiguration.touchSlop
                        val release = if (dragged) tracker.calculateVelocity(Velocity(maxFling, maxFling)) else poke
                        scope.launch { x.springHome(rangeX, release.x) { squashOnWallHit(it, hitX = true) } }
                        scope.launch { y.springHome(rangeY, release.y) { squashOnWallHit(it, hitX = false) } }

                        relaxFace = scope.launch {
                            delay(SQUEEZED_FACE_HOLD_MILLIS)
                            squeezed = false
                        }
                    }
                }
                .graphicsLayer {
                    scaleX = squashX.value
                    scaleY = squashY.value
                    transformOrigin = pivot
                    // velocity 는 Compose 상태가 아니라 이것만 읽으면 다시 계산되지 않는다.
                    // value 를 같이 읽어서 x 가 움직일 때마다 다시 계산되게 한다.
                    x.value
                    rotationZ = (x.velocity.toDp().value * ROTATION_DEG_PER_SPEED)
                        .coerceIn(-MAX_ROTATION_DEG, MAX_ROTATION_DEG)
                }
        ) {
            Box(modifier = Modifier.fillMaxSize().bobbing(enabled = progress >= FLOATING_PROGRESS)) {
                Image(
                    painter = painterResource(if (squeezed) R.drawable.duck_squeezed else R.drawable.duck),
                    contentDescription = "오리",
                    modifier = Modifier.fillMaxSize()
                )
                if (progress >= 1f) {
                    Sunglasses(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

/** 물에 뜬 것처럼 천천히 위로 까딱인다. 꺼지면 바로 제자리로 돌아간다. */
@Composable
private fun Modifier.bobbing(enabled: Boolean): Modifier {
    val bob = remember { Animatable(0f) }
    LaunchedEffect(enabled) {
        if (enabled) {
            bob.animateTo(1f, infiniteRepeatable(tween(BOB_HALF_MILLIS, easing = EaseInOut), RepeatMode.Reverse))
        } else {
            bob.snapTo(0f)
        }
    }
    return graphicsLayer {
        translationY = -BOB_RISE.toPx() * bob.value
        rotationZ = BOB_TILT_DEG * bob.value
    }
}

/** 누를 때 내는 꽥 소리. 프로토타입의 Web Audio 합성음을 미리 녹음해 둔 res/raw/quack 을 짧은 효과음용 SoundPool 로 재생한다. */
@Composable
private fun rememberQuack(): () -> Unit {
    val context = LocalContext.current
    val soundPool = remember {
        SoundPool.Builder()
            .setMaxStreams(QUACK_MAX_STREAMS)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    }
    val soundId = remember { soundPool.load(context, R.raw.quack, 1) }
    DisposableEffect(soundPool) {
        onDispose { soundPool.release() }
    }
    // 로드가 끝나기 전에 누르면 SoundPool 이 조용히 무시한다.
    return { soundPool.play(soundId, 1f, 1f, 1, 0, 1f) }
}

/** 오리 이미지와 같은 크기로 겹쳐 그린다. 처음 나타날 때 한 번 위에서 내려온다. */
@Composable
private fun Sunglasses(modifier: Modifier = Modifier) {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(entrance) {
        entrance.animateTo(1f, tween(SUNGLASSES_ENTER_MILLIS, easing = LinearEasing))
    }

    Image(
        painter = painterResource(R.drawable.duck_sunglasses),
        contentDescription = null,
        modifier = modifier.graphicsLayer {
            val landing = 1 - SUNGLASSES_EASING.transform(entrance.value)
            translationY = -size.height * SUNGLASSES_DROP_FRACTION * landing
            rotationZ = SUNGLASSES_TILT_DEG * landing
            alpha = SUNGLASSES_EASING.transform(min(entrance.value / SUNGLASSES_FADE_IN_END, 1f)).coerceIn(0f, 1f)
        }
    )
}

/**
 * 0 으로 스프링 복귀하되 ±[range] 벽에 닿으면 속도를 줄여 반대로 튕긴다.
 * 벽에 닿을 때마다 그 순간의 속력(px/s)으로 [onWallHit] 을 부른다.
 */
internal suspend fun Animatable<Float, AnimationVector1D>.springHome(
    range: Float,
    initialVelocity: Float,
    onWallHit: (speed: Float) -> Unit = {}
) {
    updateBounds(-range, range)
    var velocity = initialVelocity
    while (true) {
        val result = animateTo(0f, HOME_SPRING, velocity)
        if (result.endReason != AnimationEndReason.BoundReached) return
        val impact = result.endState.velocity
        onWallHit(abs(impact))
        velocity = -impact * WALL_RESTITUTION
    }
}

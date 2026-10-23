package com.hanix.waterwatch.ui.hydration

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.hanix.waterwatch.designsystem.R
import kotlin.math.min

// 값은 디자인 프로토타입(390pt 폭 기준)에서 옮겼다.

// 오리 이미지 비율 그대로
private val DUCK_SIZE = DpSize(132.dp, 149.dp)

// 던지는 속도 상한(dp/s)
private const val MAX_FLING_SPEED = 1800f

// 연타하면 앞 소리가 끝나기 전에 겹쳐 난다.
private const val QUACK_MAX_STREAMS = 3

// 진행률이 이 이상이면 물에 뜬 것처럼 까딱인다.
private const val FLOATING_PROGRESS = 0.55f
// 까딱임 한 방향(내려와 있다가 올라가기까지) 시간. 왕복 3.2초
private const val BOB_HALF_MILLIS = 1_600
private val BOB_RISE = 5.dp
private const val BOB_TILT_DEG = -1.5f

// 선글라스는 위에서 기울어진 채 떨어지며 살짝 지나쳤다가 제자리에 앉는다.
private const val SUNGLASSES_ENTER_MILLIS = 700
private val SUNGLASSES_EASING = CubicBezierEasing(0.3f, 1.5f, 0.5f, 1f)
private const val SUNGLASSES_DROP_FRACTION = 0.4f
private const val SUNGLASSES_TILT_DEG = -12f
// 전체 시간 중 이 지점까지 선글라스가 서서히 나타난다.
private const val SUNGLASSES_FADE_IN_END = 0.6f

/**
 * 오리 캐릭터. 누르면 꽥 소리와 함께 X 눈으로 움찔하고, 끌어다 던지면 벽에 튕기다가 가운데로 돌아온다.
 * 목표 진행률([progress], 0..1)이 오르면 물에 떠서 까딱이고, 다 채우면 선글라스를 쓴다.
 */
@Composable
fun Duck(progress: Float, modifier: Modifier = Modifier) {
    val state = rememberDuckState()

    Box(modifier = modifier.onSizeChanged { state.areaSize = it }) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { state.position.round() }
                .size(DUCK_SIZE)
                // 찌그러짐·회전은 그리기에만 적용되도록 터치 처리 뒤에 둔다.
                .duckGestures(state)
                .graphicsLayer {
                    scaleX = state.scaleX
                    scaleY = state.scaleY
                    transformOrigin = state.pivot
                    rotationZ = state.rotationDegrees()
                }
        ) {
            Box(modifier = Modifier.fillMaxSize().bobbing(enabled = progress >= FLOATING_PROGRESS)) {
                Image(
                    painter = painterResource(if (state.isSqueezed) R.drawable.duck_squeezed else R.drawable.duck),
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

@Composable
private fun rememberDuckState(): DuckState {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val quack = rememberQuack()
    return remember(scope, density) { DuckState(scope, density, DUCK_SIZE, quack) }
}

private fun Modifier.duckGestures(state: DuckState): Modifier = pointerInput(state) {
    val tracker = VelocityTracker()
    val maxFling = MAX_FLING_SPEED.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown()
        state.onPress(down.position)

        // 오리는 벽에서 멈춰도 던지는 속도는 손가락 기준이어야 해서 벽에 막히기 전 위치를 따로 쌓는다.
        val start = state.position
        var finger = start
        tracker.resetTracking()
        tracker.addPosition(down.uptimeMillis, finger)
        drag(down.id) { change ->
            finger += change.positionChange()
            state.onDrag(finger)
            tracker.addPosition(change.uptimeMillis, finger)
            change.consume()
        }

        // 탭이면 누른 반대쪽으로 살짝 밀리고, 끌었으면 손가락 속도로 던진다.
        val dragged = (finger - start).getDistance() > viewConfiguration.touchSlop
        val velocity = if (dragged) {
            tracker.calculateVelocity(Velocity(maxFling, maxFling))
        } else {
            state.pokeVelocity(down.position)
        }
        state.onRelease(velocity)
    }
}

// 물에 뜬 것처럼 천천히 위로 까딱인다. 꺼지면 바로 제자리로 돌아간다.
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

// 프로토타입의 Web Audio 합성음을 미리 녹음해 둔 res/raw/quack 을 짧은 효과음용 SoundPool 로 재생한다.
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
    // 로드가 끝나기 전에 누르면 소리가 나지 않는다.
    return { soundPool.play(soundId, 1f, 1f, 1, 0, 1f) }
}

// 오리 이미지와 같은 크기로 겹쳐 그린다. 처음 나타날 때 한 번 위에서 내려온다.
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

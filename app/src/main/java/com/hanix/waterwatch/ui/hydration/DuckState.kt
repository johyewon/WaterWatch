package com.hanix.waterwatch.ui.hydration

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.hanix.waterwatch.ui.ext.springHome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * 오리의 위치·찌그러짐·표정.
 * 누르면 X 눈으로 움찔하고, 끌면 손가락을 따라오고, 놓으면 벽에 튕기다가 가운데로 돌아온다.
 *
 * 위치는 움직일 수 있는 영역의 가운데를 0 으로 둔 px 다.
 */
@Stable
internal class DuckState(
    private val scope: CoroutineScope,
    private val density: Density,
    private val duckSize: DpSize,
    private val quack: () -> Unit
) {

    private val x = Animatable(0f)
    private val y = Animatable(0f)
    private val squashX = Animatable(1f)
    private val squashY = Animatable(1f)
    private var relaxFaceJob: Job? = null

    /** 오리가 움직일 수 있는 영역. 레이아웃이 정해지면 채워진다. */
    var areaSize: IntSize = IntSize.Zero

    val position: Offset
        get() = Offset(x.value, y.value)

    val scaleX: Float
        get() = squashX.value

    val scaleY: Float
        get() = squashY.value

    /** 찌그러짐의 기준점. 마지막으로 누른 지점이다. */
    var pivot by mutableStateOf(TransformOrigin.Center)
        private set

    /** true 면 X 눈 */
    var isSqueezed by mutableStateOf(false)
        private set

    // 영역이 오리보다 작으면 음수가 되어 coerceIn·updateBounds 가 예외를 던지므로 0 에서 막는다.
    private val rangeX: Float
        get() = ((areaSize.width - with(density) { duckSize.width.toPx() }) / 2).coerceAtLeast(0f)

    private val rangeY: Float
        get() = ((areaSize.height - with(density) { duckSize.height.toPx() }) / 2).coerceAtLeast(0f)

    /** [pressPosition] 은 오리 왼쪽 위 기준 */
    fun onPress(pressPosition: Offset) {
        relaxFaceJob?.cancel()
        isSqueezed = true
        quack()
        scope.launch {
            x.stop()
            y.stop()
        }
        pivot = TransformOrigin(
            pivotFractionX = pressPosition.x / with(density) { duckSize.width.toPx() },
            pivotFractionY = pressPosition.y / with(density) { duckSize.height.toPx() }
        )
        squash(POKE_SCALE_X, POKE_SCALE_Y)
    }

    /** [fingerPosition] 은 벽에 막히기 전 손가락 위치. 오리는 영역 안에서만 따라온다. */
    fun onDrag(fingerPosition: Offset) {
        scope.launch {
            x.snapTo(fingerPosition.x.coerceIn(-rangeX, rangeX))
            y.snapTo(fingerPosition.y.coerceIn(-rangeY, rangeY))
        }
    }

    fun onRelease(velocity: Velocity) {
        scope.launch {
            x.springHome(rangeX, velocity.x, HOME_SPRING, WALL_RESTITUTION) { squashOnWallHit(it, hitX = true) }
        }
        scope.launch {
            y.springHome(rangeY, velocity.y, HOME_SPRING, WALL_RESTITUTION) { squashOnWallHit(it, hitX = false) }
        }
        relaxFaceJob = scope.launch {
            delay(SQUEEZED_FACE_HOLD_MILLIS)
            isSqueezed = false
        }
    }

    /** 탭했을 때 밀려나는 속도. 누른 지점의 반대쪽으로, 가장자리를 누를수록 세다. */
    fun pokeVelocity(pressPosition: Offset): Velocity {
        val center = with(density) { Offset(duckSize.width.toPx(), duckSize.height.toPx()) / 2f }
        val awayFromPress = center - pressPosition
        val speed = with(density) { POKE_SPEED.dp.toPx() }
        return Velocity(awayFromPress.x / center.x * speed, awayFromPress.y / center.y * speed)
    }

    /** 좌우로 움직이는 속도만큼 진행 방향으로 기운다. */
    fun rotationDegrees(): Float {
        // velocity 는 Compose 상태가 아니라서 이것만 읽으면 다시 계산되지 않는다.
        // value 를 같이 읽어서 x 가 움직일 때마다 다시 계산되게 한다.
        x.value
        val speed = with(density) { x.velocity.toDp().value }
        return (speed * ROTATION_DEG_PER_SPEED).coerceIn(-MAX_ROTATION_DEG, MAX_ROTATION_DEG)
    }

    private fun squash(scaleX: Float, scaleY: Float) {
        scope.launch {
            squashX.snapTo(scaleX)
            squashX.animateTo(1f, SQUASH_RECOVER)
        }
        scope.launch {
            squashY.snapTo(scaleY)
            squashY.animateTo(1f, SQUASH_RECOVER)
        }
    }

    // 부딪힌 축은 눌리고 다른 축은 부푼다.
    private fun squashOnWallHit(speedPx: Float, hitX: Boolean) {
        val speed = with(density) { speedPx.toDp().value }
        if (speed <= SQUASH_MIN_SPEED) {
            return
        }
        val pressed = 1 - min(MAX_SQUASH, speed * SQUASH_PER_SPEED)
        val bulged = 1 + (1 - pressed) * SQUASH_BULGE
        if (hitX) {
            squash(pressed, bulged)
        } else {
            squash(bulged, pressed)
        }
    }

    companion object {

        // 값은 디자인 프로토타입(390pt 폭 기준)에서 옮겼다. 속도는 dp/s 다.

        // 놓으면 가운데로 돌아오면서 살짝 지나쳤다 온다. 프로토타입 스프링 k=34, c=2.8 → 감쇠비 약 0.24
        private val HOME_SPRING = spring<Float>(dampingRatio = 0.24f, stiffness = 34f)
        private const val WALL_RESTITUTION = 0.62f

        // 누르면 움찔하는 크기와 밀려나는 속도
        private const val POKE_SCALE_X = 0.9f
        private const val POKE_SCALE_Y = 0.86f
        private const val POKE_SPEED = 90f

        // 이 속도 이상으로 벽에 부딪히면 속도에 비례해 MAX_SQUASH 까지 찌그러진다.
        private const val SQUASH_MIN_SPEED = 60f
        private const val SQUASH_PER_SPEED = 1f / 2600
        private const val MAX_SQUASH = 0.22f
        // 찌그러진 축이 줄어든 만큼에 이 비율을 곱해서 다른 축을 늘린다.
        private const val SQUASH_BULGE = 0.6f
        private val SQUASH_RECOVER = spring<Float>(stiffness = Spring.StiffnessLow)

        // 좌우 속도 1dp/s 당 기우는 각도와 최대 각도
        private const val ROTATION_DEG_PER_SPEED = 0.04f
        private const val MAX_ROTATION_DEG = 22f

        // 손을 뗀 뒤 이만큼 더 X 눈을 하고 있다가 돌아온다.
        private const val SQUEEZED_FACE_HOLD_MILLIS = 700L
    }
}

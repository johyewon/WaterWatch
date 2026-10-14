package com.hanix.waterwatch.wear.ui.hydration

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.DpSize
import kotlin.math.abs
import kotlin.math.min

/**
 * 워치 오리의 위치·찌그러짐·표정.
 * 화면이 둥글어서 x·y 를 따로 튕기는 폰 방식 대신, 프로토타입처럼 매 프레임 [step] 으로 원형 벽에 2D 로 튕긴다.
 *
 * 위치는 화면 가운데를 0 으로 둔 dp, 속도는 dp/s 다.
 */
@Stable
internal class WearDuckState(private val duckSize: DpSize) {

    private var velocity = Offset.Zero
    private var dragging = false
    private var squeezedFaceSecondsLeft = 0f

    /** 오리 중심이 가운데에서 벗어날 수 있는 거리. 레이아웃이 정해지면 채워진다. */
    var maxDistance: Float = 0f

    var position by mutableStateOf(Offset.Zero)
        private set

    var scaleX by mutableFloatStateOf(1f)
        private set

    var scaleY by mutableFloatStateOf(1f)
        private set

    /** 좌우로 움직이는 속도만큼 진행 방향으로 기운다. */
    var rotationDegrees by mutableFloatStateOf(0f)
        private set

    /** 찌그러짐의 기준점. 마지막으로 누른 지점이다. */
    var pivot by mutableStateOf(TransformOrigin.Center)
        private set

    /** true 면 X 눈 */
    var isSqueezed by mutableStateOf(false)
        private set

    /** true 면 가운데 멈춰 있어서 [step] 을 부를 필요가 없다. */
    var isResting by mutableStateOf(true)
        private set

    /** [pressPosition] 은 오리 왼쪽 위 기준 */
    fun onPress(pressPosition: Offset) {
        isResting = false
        dragging = true
        velocity = Offset.Zero
        isSqueezed = true
        pivot = TransformOrigin(pressPosition.x / duckSize.width.value, pressPosition.y / duckSize.height.value)
        scaleX = POKE_SCALE_X
        scaleY = POKE_SCALE_Y
    }

    /** [fingerPosition] 은 벽에 막히기 전 손가락 위치. 오리는 벽 안에서만 따라온다. */
    fun onDrag(fingerPosition: Offset) {
        position = fingerPosition
        bounceOffWall()
    }

    fun onRelease(velocity: Offset) {
        dragging = false
        this.velocity = velocity
        squeezedFaceSecondsLeft = SQUEEZED_FACE_HOLD_SECONDS
    }

    /** 탭했을 때 밀려나는 속도. 누른 지점의 반대쪽으로, 가장자리를 누를수록 세다. */
    fun pokeVelocity(pressPosition: Offset): Offset {
        val center = Offset(duckSize.width.value, duckSize.height.value) / 2f
        val awayFromPress = center - pressPosition
        return Offset(awayFromPress.x / center.x, awayFromPress.y / center.y) * POKE_SPEED
    }

    fun step(elapsedSeconds: Float) {
        val dt = min(elapsedSeconds, MAX_STEP_SECONDS)
        if (!dragging) {
            velocity += (position * -HOME_STIFFNESS - velocity * HOME_DAMPING) * dt
            position += velocity * dt
            bounceOffWall()
        }

        val recover = min(1f, dt * SQUASH_RECOVER_PER_SECOND)
        scaleX += (1 - scaleX) * recover
        scaleY += (1 - scaleY) * recover
        rotationDegrees = (velocity.x * ROTATION_DEG_PER_SPEED).coerceIn(-MAX_ROTATION_DEG, MAX_ROTATION_DEG)

        if (isSqueezed && !dragging) {
            squeezedFaceSecondsLeft -= dt
            isSqueezed = squeezedFaceSecondsLeft > 0
        }

        if (!dragging && !isSqueezed && isSettled()) {
            rest()
        }
    }

    // 벽 밖으로 나갔으면 벽 위로 되돌리고, 벽 쪽으로 가던 속도는 반발 계수만큼 남기고 뒤집는다.
    private fun bounceOffWall() {
        val distance = position.getDistance()
        if (distance <= maxDistance) {
            return
        }
        val normal = position / distance
        position = normal * maxDistance

        val towardWall = velocity.x * normal.x + velocity.y * normal.y
        if (towardWall <= 0) {
            return
        }
        if (towardWall > SQUASH_MIN_SPEED) {
            squashOnWallHit(towardWall, hitSideways = abs(normal.x) > abs(normal.y))
        }
        velocity -= normal * ((1 + WALL_RESTITUTION) * towardWall)
    }

    // 부딪힌 축은 눌리고 다른 축은 부푼다.
    private fun squashOnWallHit(speed: Float, hitSideways: Boolean) {
        val squash = min(MAX_SQUASH, speed * SQUASH_PER_SPEED)
        val pressed = 1 - squash
        val bulged = 1 + squash * SQUASH_BULGE
        scaleX = if (hitSideways) pressed else bulged
        scaleY = if (hitSideways) bulged else pressed
    }

    private fun isSettled(): Boolean =
        position.getDistance() < REST_THRESHOLD &&
            velocity.getDistance() < REST_THRESHOLD &&
            abs(1 - scaleX) < REST_THRESHOLD &&
            abs(1 - scaleY) < REST_THRESHOLD

    private fun rest() {
        position = Offset.Zero
        velocity = Offset.Zero
        scaleX = 1f
        scaleY = 1f
        rotationDegrees = 0f
        isResting = true
    }

    companion object {

        // 값은 디자인 프로토타입(갤럭시 워치 384px 보드)에서 옮겼다. 길이·속도는 보드 값의 절반을 dp 로 쓴다.

        // 프레임이 밀려도 한 번에 이 시간 이상 진행하지 않는다.
        private const val MAX_STEP_SECONDS = 0.033f

        // 놓으면 가운데로 돌아오면서 살짝 지나쳤다 온다. 프로토타입 스프링 k=34, c=2.8
        private const val HOME_STIFFNESS = 34f
        private const val HOME_DAMPING = 2.8f
        private const val WALL_RESTITUTION = 0.62f

        // 누르면 움찔하는 크기와 밀려나는 속도
        private const val POKE_SCALE_X = 0.9f
        private const val POKE_SCALE_Y = 0.86f
        private const val POKE_SPEED = 45f

        // 이 속도 이상으로 벽에 부딪히면 속도에 비례해 MAX_SQUASH 까지 찌그러진다.
        private const val SQUASH_MIN_SPEED = 30f
        private const val SQUASH_PER_SPEED = 1f / 1300
        private const val MAX_SQUASH = 0.22f
        // 찌그러진 축이 줄어든 만큼에 이 비율을 곱해서 다른 축을 늘린다.
        private const val SQUASH_BULGE = 0.6f
        private const val SQUASH_RECOVER_PER_SECOND = 14f

        // 좌우 속도 1dp/s 당 기우는 각도와 최대 각도
        private const val ROTATION_DEG_PER_SPEED = 0.08f
        private const val MAX_ROTATION_DEG = 22f

        // 손을 뗀 뒤 이만큼 더 X 눈을 하고 있다가 돌아온다.
        private const val SQUEEZED_FACE_HOLD_SECONDS = 0.7f

        // 위치(dp)·속도(dp/s)·찌그러짐이 모두 이보다 작으면 멈춘 것으로 본다.
        private const val REST_THRESHOLD = 0.01f
    }
}

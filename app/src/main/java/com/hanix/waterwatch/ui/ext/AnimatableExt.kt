package com.hanix.waterwatch.ui.ext

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.SpringSpec
import kotlin.math.abs

/**
 * 0 으로 스프링 복귀하되 ±[range] 벽에 닿으면 [restitution] 만큼 속도를 남기고 반대로 튕긴다.
 * 벽에 닿을 때마다 그 순간의 속력(px/s)으로 [onWallHit] 을 호출한다.
 */
internal suspend fun Animatable<Float, AnimationVector1D>.springHome(
    range: Float,
    initialVelocity: Float,
    spring: SpringSpec<Float>,
    restitution: Float,
    onWallHit: (speed: Float) -> Unit = {}
) {
    updateBounds(-range, range)
    var velocity = initialVelocity
    while (true) {
        val result = animateTo(0f, spring, velocity)
        if (result.endReason != AnimationEndReason.BoundReached) {
            return
        }
        val impact = result.endState.velocity
        onWallHit(abs(impact))
        velocity = -impact * restitution
    }
}

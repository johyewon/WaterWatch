package com.hanix.waterwatch.ui.hydration

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpringHomeTest {

    @Test
    fun `벽 쪽으로 던지면 튕긴 뒤 가운데로 돌아온다`() = runBlocking(FakeFrameClock()) {
        val position = Animatable(90f)
        val wallHits = mutableListOf<Float>()

        position.springHome(range = 100f, initialVelocity = 2_000f) { wallHits += it }

        assertTrue(wallHits.isNotEmpty())
        assertEquals(0f, position.value, 0.5f)
    }

    @Test
    fun `벽에 안 닿을 만큼 살살 놓으면 튕기지 않는다`() = runBlocking(FakeFrameClock()) {
        val position = Animatable(10f)
        val wallHits = mutableListOf<Float>()

        position.springHome(range = 100f, initialVelocity = 0f) { wallHits += it }

        assertTrue(wallHits.isEmpty())
        assertEquals(0f, position.value, 0.5f)
    }
}

/** 호출될 때마다 16ms 씩 흐르는 프레임 시계 */
private class FakeFrameClock : MonotonicFrameClock {
    private var frameTimeNanos = 0L

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        frameTimeNanos += 16_000_000L
        return onFrame(frameTimeNanos)
    }
}

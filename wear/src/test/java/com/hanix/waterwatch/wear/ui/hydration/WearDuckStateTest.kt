package com.hanix.waterwatch.wear.ui.hydration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WearDuckStateTest {

    @Test
    fun `대각선으로 던지면 둥근 벽 안에서 튕긴 뒤 가운데에 멈춘다`() {
        val state = duckState()
        var farthest = 0f
        var squashed = false

        state.onPress(Offset(14f, 15.75f))
        state.onRelease(Offset(600f, 600f))
        repeat(FRAMES) {
            state.step(FRAME_SECONDS)
            farthest = maxOf(farthest, state.position.getDistance())
            squashed = squashed || state.scaleX > 1f || state.scaleY > 1f
        }

        assertEquals(MAX_DISTANCE, farthest, 0.01f)
        assertTrue(squashed)
        assertTrue(state.isResting)
        assertEquals(Offset.Zero, state.position)
    }

    @Test
    fun `끌면 벽 밖으로는 따라가지 않는다`() {
        val state = duckState()

        state.onPress(Offset(14f, 15.75f))
        state.onDrag(Offset(300f, 0f))

        assertEquals(Offset(MAX_DISTANCE, 0f), state.position)
    }

    @Test
    fun `탭하면 X 눈을 했다가 손 떼고 700ms 뒤 돌아온다`() {
        val state = duckState()

        state.onPress(Offset(14f, 15.75f))
        state.onRelease(Offset.Zero)
        repeat(30) { state.step(FRAME_SECONDS) }
        assertTrue(state.isSqueezed)

        repeat(20) { state.step(FRAME_SECONDS) }
        assertFalse(state.isSqueezed)
    }

    private fun duckState() = WearDuckState(DpSize(28.dp, 31.5.dp)).apply { maxDistance = MAX_DISTANCE }
}

// 작은 원형 워치(192dp)에서 오리가 움직일 수 있는 거리
private const val MAX_DISTANCE = 82f
private const val FRAME_SECONDS = 1f / 60
private const val FRAMES = 60 * 20

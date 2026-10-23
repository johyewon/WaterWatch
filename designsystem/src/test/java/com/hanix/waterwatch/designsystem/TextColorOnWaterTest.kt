package com.hanix.waterwatch.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class TextColorOnWaterTest {

    @Test
    fun `하루 시작에는 위아래 글자가 모두 어두운색이다`() {
        assertEquals(Ink, readableTextColor(HydrationBackgroundSpec.Phone.headerBackgroundColor(0f), Ink))
        assertEquals(UndoContent, readableTextColor(footerBackgroundColor(0f), UndoContent))
    }

    @Test
    fun `목표를 채우면 위아래 글자가 모두 흰색이다`() {
        assertEquals(Color.White, readableTextColor(HydrationBackgroundSpec.Phone.headerBackgroundColor(1f), Ink))
        assertEquals(Color.White, readableTextColor(footerBackgroundColor(1f), UndoContent))
    }

    @Test
    fun `물이 위쪽 글자 높이를 넘으면 위쪽 배경은 물 윗면 색이다`() {
        // 폰은 0.97 까지만 차오르고 0.72 를 넘으면 덮이므로 진행률 0.75 부터 물이다.
        assertEquals(WaterFullTop, HydrationBackgroundSpec.Phone.headerBackgroundColor(1f))
        assertEquals(BackgroundLightTop, HydrationBackgroundSpec.Phone.headerBackgroundColor(0f))
    }

    @Test
    fun `워치도 하루 시작에는 어두운색, 목표를 채우면 흰색이다`() {
        assertEquals(Ink, readableTextColor(HydrationBackgroundSpec.Watch.headerBackgroundColor(0f), Ink))
        assertEquals(Color.White, readableTextColor(HydrationBackgroundSpec.Watch.headerBackgroundColor(1f), Ink))
    }
}

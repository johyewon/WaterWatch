package com.hanix.waterwatch.ui.theme

import androidx.compose.material3.Typography
import com.hanix.waterwatch.designsystem.Pretendard

private val Default = Typography()

/** Material 기본 크기는 그대로 두고 글꼴만 디자인 프로토타입의 Pretendard 로 바꾼다. */
val Typography = Typography(
    displayLarge = Default.displayLarge.copy(fontFamily = Pretendard),
    displayMedium = Default.displayMedium.copy(fontFamily = Pretendard),
    displaySmall = Default.displaySmall.copy(fontFamily = Pretendard),
    headlineLarge = Default.headlineLarge.copy(fontFamily = Pretendard),
    headlineMedium = Default.headlineMedium.copy(fontFamily = Pretendard),
    headlineSmall = Default.headlineSmall.copy(fontFamily = Pretendard),
    titleLarge = Default.titleLarge.copy(fontFamily = Pretendard),
    titleMedium = Default.titleMedium.copy(fontFamily = Pretendard),
    titleSmall = Default.titleSmall.copy(fontFamily = Pretendard),
    bodyLarge = Default.bodyLarge.copy(fontFamily = Pretendard),
    bodyMedium = Default.bodyMedium.copy(fontFamily = Pretendard),
    bodySmall = Default.bodySmall.copy(fontFamily = Pretendard),
    labelLarge = Default.labelLarge.copy(fontFamily = Pretendard),
    labelMedium = Default.labelMedium.copy(fontFamily = Pretendard),
    labelSmall = Default.labelSmall.copy(fontFamily = Pretendard)
)

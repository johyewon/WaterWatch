package com.hanix.waterwatch.wear.ui.theme

import androidx.wear.compose.material3.Typography
import com.hanix.waterwatch.designsystem.Pretendard

private val Default = Typography()

/*
 * Wear Material 기본 크기는 그대로 두고 글꼴만 디자인 프로토타입의 Pretendard 로 바꾼다.
 * Typography(defaultFontFamily) 는 쓰지 않는다. 기본 스타일마다 기기 글꼴(roboto-flex)이 이미 지정돼 있어서
 * 글꼴이 비어 있는 스타일에만 적용되는 그 값은 무시된다.
 * ponytail: 곡선 글자(arc*)는 쓰지 않아서 기본 글꼴 그대로 둔다. 쓰게 되면 같이 바꾼다.
 */
val WearTypography = Typography(
    displayLarge = Default.displayLarge.copy(fontFamily = Pretendard),
    displayMedium = Default.displayMedium.copy(fontFamily = Pretendard),
    displaySmall = Default.displaySmall.copy(fontFamily = Pretendard),
    titleLarge = Default.titleLarge.copy(fontFamily = Pretendard),
    titleMedium = Default.titleMedium.copy(fontFamily = Pretendard),
    titleSmall = Default.titleSmall.copy(fontFamily = Pretendard),
    labelLarge = Default.labelLarge.copy(fontFamily = Pretendard),
    labelMedium = Default.labelMedium.copy(fontFamily = Pretendard),
    labelSmall = Default.labelSmall.copy(fontFamily = Pretendard),
    bodyLarge = Default.bodyLarge.copy(fontFamily = Pretendard),
    bodyMedium = Default.bodyMedium.copy(fontFamily = Pretendard),
    bodySmall = Default.bodySmall.copy(fontFamily = Pretendard),
    bodyExtraSmall = Default.bodyExtraSmall.copy(fontFamily = Pretendard),
    numeralExtraLarge = Default.numeralExtraLarge.copy(fontFamily = Pretendard),
    numeralLarge = Default.numeralLarge.copy(fontFamily = Pretendard),
    numeralMedium = Default.numeralMedium.copy(fontFamily = Pretendard),
    numeralSmall = Default.numeralSmall.copy(fontFamily = Pretendard),
    numeralExtraSmall = Default.numeralExtraSmall.copy(fontFamily = Pretendard)
)

package com.hanix.waterwatch.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

/** 기록 토스트 문구. 디자인 프로토타입 값 그대로, 가이드가 나오면 바꾼다. */
val ToastTextStyle = TextStyle(
    fontSize = 15.sp,
    fontWeight = FontWeight.SemiBold
)

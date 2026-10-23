package com.hanix.waterwatch.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * 디자인 프로토타입 글꼴. 굵기 하나마다 파일을 넣는 대신 variable 폰트 한 파일에서 굵기를 골라 쓴다.
 * 라이선스(SIL OFL 1.1)는 assets/licenses/pretendard_ofl.txt 로 앱에 같이 들어간다.
 */
private val PRETENDARD_WEIGHTS = listOf(
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
    FontWeight.ExtraBold
)

val Pretendard = FontFamily(
    PRETENDARD_WEIGHTS.map { weight ->
        Font(
            resId = R.font.pretendard_variable,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
        )
    }
)

/** 기록 토스트 문구. 디자인 프로토타입 값 그대로, 가이드가 나오면 바꾼다. */
val ToastTextStyle = TextStyle(
    fontFamily = Pretendard,
    fontSize = 15.sp,
    fontWeight = FontWeight.SemiBold
)

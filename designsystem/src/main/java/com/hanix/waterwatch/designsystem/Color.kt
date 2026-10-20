package com.hanix.waterwatch.designsystem

import androidx.compose.ui.graphics.Color

/*
 * 폰·워치가 같이 쓰는 색. 디자인 가이드가 아직 없어서 디자인 프로토타입 값을 옮겨 왔다.
 * 프로토타입에서 폰·워치 값이 눈으로 구분되지 않을 만큼만 달랐던 배경은 폰 값으로 합쳤다.
 * 가이드가 나오면 아래 값만 바꾼다.
 */

/** 배경. 물을 마실수록 Light 위로 Deep 이 덮인다. */
val BackgroundLightTop = Color(0xFFF5FAFF)
val BackgroundLightMiddle = Color(0xFFECF4FC)
val BackgroundLightBottom = Color(0xFFDFECF9)
val BackgroundDeepTop = Color(0xFF7398DA)
val BackgroundDeepMiddle = Color(0xFF6289D2)
val BackgroundDeepBottom = Color(0xFF557DCB)

/** 물. 진행률 0 이면 Empty, 다 채우면 Full 색이다. */
val WaterEmptyTop = Color(0xFFB3D9F8)
val WaterEmptyMiddle = Color(0xFF97CBF5)
val WaterEmptyBottom = Color(0xFF78B4EE)
val WaterFullTop = Color(0xFF2E5BB8)
val WaterFullMiddle = Color(0xFF1F4AA3)
val WaterFullBottom = Color(0xFF122C7A)

/** 기본 글자색 */
val Ink = Color(0xFF0A2540)

/** 강조색. 체크 표시 등 */
val Accent = Color(0xFF2149A6)

val ToastBackground = Color.White

/** 기록 버튼 */
val ButtonContainer = Color.White
val ButtonContent = Color(0xFF333D4B)
val ButtonBorder = Color(0xFFCDE6FB)
val ButtonIcon = Color(0xFF6B7684)
val ButtonMoreIcon = Color(0xFF4E5968)

/** 폰 바텀시트 */
val BottomSheetBackground = Color.White
val BottomSheetScrim = Color(0x660A1E46)
val BottomSheetHandle = Color(0xFFD1D6DB)
val BottomSheetTitle = Color(0xFF191F28)
val BottomSheetSecondaryButton = Color(0xFFF2F4F6)

/** 워치 더보기 시트 */
val SheetBackground = Color(0xF508122C)
val SheetButton = Color.White.copy(alpha = 0.14f)
val SheetContent = Color.White
val SheetSubtleContent = Color(0xFFB8C7E6)
val SheetPrimaryContainer = Color.White
val SheetPrimaryContent = Color(0xFF16348A)

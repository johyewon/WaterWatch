package com.hanix.waterwatch.wear.data.source

/** 폰이 보내준 오늘 섭취 상태 */
data class TodayIntake(
    val totalMl: Int,
    val goalMl: Int
)

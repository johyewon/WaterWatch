package com.hanix.waterwatch.data.repo

import com.hanix.waterwatch.data.source.HealthConnectDataSource

/** Health Connect 를 단일 저장소로 쓴다. */
class HydrationRepository(
    private val healthConnectDataSource: HealthConnectDataSource
) {

    val isHealthConnectAvailable: Boolean
        get() = healthConnectDataSource.isAvailable

    /** 앱 동작에 필요한 최소 권한. 이게 false 면 조회도 기록도 못 한다. */
    suspend fun hasPermissions(): Boolean = healthConnectDataSource.hasHydrationPermissions()

    suspend fun todayTotalMl(): Double? = healthConnectDataSource.readTodayTotalMl()
}

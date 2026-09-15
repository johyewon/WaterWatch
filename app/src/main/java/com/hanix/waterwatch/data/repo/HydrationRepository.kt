package com.hanix.waterwatch.data.repo

import android.util.Log
import com.hanix.waterwatch.data.source.HealthConnectDataSource
import com.hanix.waterwatch.data.source.WearSyncDataSource

/** Health Connect 를 단일 저장소로 쓰고, 조회 결과를 워치에 함께 반영한다. */
class HydrationRepository(
    private val healthConnectDataSource: HealthConnectDataSource,
    private val wearSyncDataSource: WearSyncDataSource
) {

    val isHealthConnectAvailable: Boolean
        get() = healthConnectDataSource.isAvailable

    /** 앱 동작에 필요한 최소 권한. 이게 false 면 조회도 기록도 못 한다. */
    suspend fun hasPermissions(): Boolean = healthConnectDataSource.hasHydrationPermissions()

    /** 조회한 값은 곧바로 워치에 반영한다. 폰만 최신이고 워치가 낡은 값을 드는 상태를 안 만들기 위해. */
    suspend fun todayTotalMl(): Double? =
        healthConnectDataSource.readTodayTotalMl().also { publishToWear(it) }

    suspend fun isWatchConnected(): Boolean = wearSyncDataSource.isWatchConnected()

    /** 워치 미페어링·Wearable API 없음은 폰 기능과 무관하므로 조회 실패로 올리지 않는다. */
    private suspend fun publishToWear(ml: Double?) {
        runCatching { wearSyncDataSource.publishTodayTotal(ml) }
            .onFailure { Log.w(TAG, "워치 발행 실패 — 폰 조회 결과는 유효", it) }
    }

    companion object {
        private const val TAG = "WaterWatch/Wear"
    }
}

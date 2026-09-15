package com.hanix.waterwatch.data.source

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.LocalDateTime

/** Health Connect 의 수분 섭취 데이터 접근 */
interface HealthConnectDataSource {

    val isAvailable: Boolean

    /** [HYDRATION_PERMISSIONS] 보유 여부 */
    suspend fun hasHydrationPermissions(): Boolean

    /** 오늘 0시부터 지금까지의 총량(ml). 기록이 없으면 null */
    suspend fun readTodayTotalMl(): Double?

    companion object {

        /** 없으면 조회도 기록도 불가능한 최소 권한 */
        val HYDRATION_PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(HydrationRecord::class),
            HealthPermission.getWritePermission(HydrationRecord::class)
        )
    }
}

internal class HealthConnectDataSourceImpl(
    private val context: Context
) : HealthConnectDataSource {

    // getOrCreate 는 Health Connect 가 없거나 업데이트가 필요한 기기에서 실패하므로 먼저 확인한다.
    override val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    // isAvailable 을 확인한 뒤에만 접근한다. 생성 시점에 터지지 않도록 지연 생성.
    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    override suspend fun hasHydrationPermissions(): Boolean =
        client.permissionController.getGrantedPermissions()
            .containsAll(HealthConnectDataSource.HYDRATION_PERMISSIONS)

    override suspend fun readTodayTotalMl(): Double? {
        // 직접 합산하면 다른 앱이 기록한 값이 누락되므로 집계로 조회한다.
        val response = client.aggregate(
            AggregateRequest(
                metrics = setOf(HydrationRecord.VOLUME_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(
                    LocalDate.now().atStartOfDay(),
                    LocalDateTime.now()
                )
            )
        )
        return response[HydrationRecord.VOLUME_TOTAL]?.inMilliliters
    }
}

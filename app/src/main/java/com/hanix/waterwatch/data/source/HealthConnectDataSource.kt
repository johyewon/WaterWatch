package com.hanix.waterwatch.data.source

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Volume
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Health Connect 의 수분 섭취 데이터 접근 */
interface HealthConnectDataSource {

    val isAvailable: Boolean

    /** [HYDRATION_PERMISSIONS] 보유 여부 */
    suspend fun hasHydrationPermissions(): Boolean

    /** [REQUESTED_PERMISSIONS] 보유 여부 */
    suspend fun hasAllRequestedPermissions(): Boolean

    /** 오늘 0시부터 지금까지의 총량(ml). 기록이 없으면 null */
    suspend fun readTodayTotalMl(): Double?

    suspend fun writeHydrationMl(ml: Double)

    companion object {

        /** 없으면 조회도 기록도 불가능한 최소 권한 */
        val HYDRATION_PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(HydrationRecord::class),
            HealthPermission.getWritePermission(HydrationRecord::class)
        )

        /*
         * 백그라운드 읽기는 워치가 기록할 때(폰 앱이 안 떠 있을 때) 총량을 다시 집계하는 데 필요하다.
         * 없어도 폰 화면은 정상 동작하므로 HYDRATION_PERMISSIONS 와 달리 필수로 보지 않는다.
         */
        val REQUESTED_PERMISSIONS: Set<String> =
            HYDRATION_PERMISSIONS + HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
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
        hasGranted(HealthConnectDataSource.HYDRATION_PERMISSIONS)

    override suspend fun hasAllRequestedPermissions(): Boolean =
        hasGranted(HealthConnectDataSource.REQUESTED_PERMISSIONS)

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

    override suspend fun writeHydrationMl(ml: Double) {
        val end = Instant.now()

        // HydrationRecord 는 startTime < endTime 을 요구한다(같으면 IllegalArgumentException).
        // 한 번 마신 행위라 구간 길이 자체는 의미가 없어 최소 구간으로 넣는다.
        val start = end.minusSeconds(MINIMUM_RECORD_SECONDS)
        val offset = ZoneId.systemDefault().rules.getOffset(end)

        client.insertRecords(
            listOf(
                HydrationRecord(
                    startTime = start,
                    startZoneOffset = offset,
                    endTime = end,
                    endZoneOffset = offset,
                    volume = Volume.milliliters(ml),
                    metadata = Metadata.manualEntry()
                )
            )
        )
    }

    private suspend fun hasGranted(permissions: Set<String>): Boolean =
        client.permissionController.getGrantedPermissions().containsAll(permissions)

    companion object {
        private const val MINIMUM_RECORD_SECONDS = 1L
    }
}

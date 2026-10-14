package com.hanix.waterwatch.data.repo

import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.hanix.waterwatch.data.source.GoalDataSource
import com.hanix.waterwatch.data.source.HealthConnectDataSource
import com.hanix.waterwatch.data.source.WearSyncDataSource
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first

/** 섭취 기록은 Health Connect 를 단일 저장소로 쓰고, 조회 결과를 워치에 함께 반영한다. */
class HydrationRepository(
    private val healthConnectDataSource: HealthConnectDataSource,
    private val wearSyncDataSource: WearSyncDataSource,
    private val goalDataSource: GoalDataSource
) {

    /*
     * 워치에서 온 기록은 서비스가 이 repository 로 쓰기 때문에, 폰 화면은 이 신호를 받아야 다시 읽을 수 있다.
     * 값은 Health Connect 에만 두고 여기서는 바뀌었다는 사실만 알린다.
     * 받는 쪽은 어차피 최신 값을 다시 읽으므로 밀린 신호는 하나만 남긴다.
     */
    private val _intakeChanged = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val intakeChanged: SharedFlow<Unit> = _intakeChanged.asSharedFlow()

    val goalMl: Flow<Int>
        get() = goalDataSource.goalMl

    val isHealthConnectAvailable: Boolean
        get() = healthConnectDataSource.isAvailable

    /** 앱 동작에 필요한 최소 권한. 이게 false 면 조회도 기록도 못 한다. */
    suspend fun hasPermissions(): Boolean = healthConnectDataSource.hasHydrationPermissions()

    /** 백그라운드 읽기까지 포함. 최소 권한만 있는 상태를 구분해 권한 요청을 다시 띄우는 데 쓴다. */
    suspend fun hasAllRequestedPermissions(): Boolean =
        healthConnectDataSource.hasAllRequestedPermissions()

    /** 조회한 값은 곧바로 워치에 반영한다. 폰만 최신이고 워치가 낡은 값을 드는 상태를 안 만들기 위해. */
    suspend fun todayTotalMl(): Double? =
        healthConnectDataSource.readTodayTotalMl().also { publishToWear(it) }

    suspend fun recordIntake(ml: Double) {
        healthConnectDataSource.writeHydrationMl(ml)
        _intakeChanged.tryEmit(Unit)
    }

    suspend fun undoLastIntake(): Boolean =
        healthConnectDataSource.deleteLatestTodayRecord().also { deleted ->
            if (deleted) _intakeChanged.tryEmit(Unit)
        }

    suspend fun isWatchConnected(): Boolean = wearSyncDataSource.isWatchConnected()

    suspend fun sendRecordResult(nodeId: String, success: Boolean) {
        wearSyncDataSource.sendRecordResult(nodeId = nodeId, success = success)
    }

    // ponytail: 목표는 총량을 조회할 때만 같이 보낸다. 폰에서 목표를 바꾸는 화면이 생기면 그때도 발행해야 한다.
    /** 워치 미페어링·Wearable API 없음은 폰 기능과 무관하므로 조회 실패로 올리지 않는다. */
    private suspend fun publishToWear(totalMl: Double?) {
        runCatching { wearSyncDataSource.publishToday(totalMl, goalDataSource.goalMl.first()) }
            .onFailure {
                if (it is ApiException && it.statusCode == CommonStatusCodes.API_NOT_CONNECTED) {
                    Log.i(TAG, "워치 없음 — 발행 건너뜀")
                } else {
                    Log.w(TAG, "워치 발행 실패 — 폰 조회 결과는 유효", it)
                }
            }
    }

    companion object {
        private const val TAG = "WaterWatch/Wear"
    }
}

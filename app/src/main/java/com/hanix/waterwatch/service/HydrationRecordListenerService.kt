package com.hanix.waterwatch.service

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.hanix.waterwatch.common.PATH_LOG_HYDRATION
import com.hanix.waterwatch.common.PATH_UNDO_HYDRATION
import com.hanix.waterwatch.locator.ServiceLocator
import kotlinx.coroutines.runBlocking

/** 워치가 보낸 기록·취소 요청을 Health Connect 에 반영하고, 갱신된 총량을 다시 워치로 내려보낸다. */
class HydrationRecordListenerService : WearableListenerService() {

    private val hydrationRepository get() = ServiceLocator.hydrationRepository

    // 콜백은 이미 백그라운드 스레드이고, 반환하면 서비스가 종료될 수 있어 여기서 끝까지 처리한다.
    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            PATH_LOG_HYDRATION -> runBlocking { record(event) }
            PATH_UNDO_HYDRATION -> runBlocking { undo() }
        }
    }

    private suspend fun record(event: MessageEvent) {
        val payload = String(event.data)
        val ml = payload.toDoubleOrNull()
        if (ml == null || ml <= 0) {
            Log.w(TAG, "payload 해석 실패 — payload=$payload")
            sendResult(event, success = false)
            return
        }
        if (hydrationRepository.isHealthConnectAvailable.not()) {
            Log.w(TAG, "Health Connect 사용 불가 — 기록 건너뜀 ml=$ml")
            sendResult(event, success = false)
            return
        }

        val recorded = runCatching { hydrationRepository.recordIntake(ml) }
            .onSuccess { Log.i(TAG, "기록 완료 ml=$ml") }
            .onFailure { Log.e(TAG, "기록 실패 ml=$ml", it) }

        // 워치에 알리는 건 '기록' 성공 여부다. 총량 재조회가 실패해도 물은 이미 기록됐다.
        sendResult(event, recorded.isSuccess)
        if (recorded.isFailure) return

        publishTodayToWear()
    }

    // ponytail: 취소는 결과를 회신하지 않는다. 워치는 총량이 줄어드는 것으로 확인한다. 실패 안내가 필요해지면 회신 경로 추가.
    private suspend fun undo() {
        if (hydrationRepository.isHealthConnectAvailable.not()) {
            Log.w(TAG, "Health Connect 사용 불가 — 취소 건너뜀")
            return
        }

        val deleted = runCatching { hydrationRepository.undoLastIntake() }
            .onSuccess { Log.i(TAG, "취소 deleted=$it") }
            .onFailure { Log.e(TAG, "취소 실패", it) }
            .getOrDefault(false)
        if (deleted) publishTodayToWear()
    }

    // 백그라운드 읽기 권한이 없으면 여기서 실패한다. 기록·취소 자체는 유효하므로 로그만 남긴다.
    private suspend fun publishTodayToWear() {
        runCatching { hydrationRepository.todayTotalMl() }
            .onSuccess { Log.i(TAG, "총량 갱신 — $it") }
            .onFailure { Log.e(TAG, "총량 재조회 실패 — 워치 값이 낡을 수 있음", it) }
    }

    private suspend fun sendResult(event: MessageEvent, success: Boolean) {
        hydrationRepository.sendRecordResult(nodeId = event.sourceNodeId, success = success)
    }

    companion object {
        private const val TAG = "WaterWatch/Wear"
    }
}

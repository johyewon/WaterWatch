package com.hanix.waterwatch.data.source

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.hanix.waterwatch.common.KEY_EPOCH_DAY
import com.hanix.waterwatch.common.KEY_TOTAL_ML
import com.hanix.waterwatch.common.PATH_TODAY_TOTAL
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/** 워치로의 수분 섭취 데이터 전송 */
interface WearSyncDataSource {

    /** 오늘 총량을 워치가 구독하는 DataItem 으로 발행. 기록이 없으면 ml 은 null */
    suspend fun publishTodayTotal(ml: Double?)

    suspend fun isWatchConnected(): Boolean
}

internal class WearSyncDataSourceImpl(context: Context) : WearSyncDataSource {

    private val dataClient = Wearable.getDataClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    /*
     * 총량·날짜가 모두 이전과 같으면 DataItem 이 안 바뀌어 워치에 이벤트가 가지 않는다.
     * 워치가 표시할 내용이 동일하므로 의도된 동작이다.
     * 연결이 끊겨 있어도 DataClient 가 버퍼링해두고 재연결 시 싱크한다.
     */
    override suspend fun publishTodayTotal(ml: Double?) {
        val request = PutDataMapRequest.create(PATH_TODAY_TOTAL)
            .apply {
                dataMap.putInt(KEY_TOTAL_ML, (ml ?: 0.0).toInt())
                dataMap.putLong(KEY_EPOCH_DAY, LocalDate.now().toEpochDay())
            }
            .asPutDataRequest()
            .setUrgent()

        dataClient.putDataItem(request).await()
    }

    /*
     * 발행 성공 여부로는 알 수 없다. putDataItem 은 워치가 끊겨 있어도 버퍼링되며 성공하기 때문.
     * 워치 미지원 폰(컴패니언 앱 미설치)은 예외가 나므로 미연결로 본다.
     *
     * ponytail: 호출 시점의 스냅샷. 연결 변화를 실시간으로 반영하려면 WearableListenerService 의
     * onCapabilityChanged 로 옮겨야 한다.
     */
    override suspend fun isWatchConnected(): Boolean =
        runCatching { nodeClient.connectedNodes.await().isNotEmpty() }
            .onFailure { Log.w(TAG, "연결 노드 조회 실패", it) }
            .getOrDefault(false)

    companion object {
        private const val TAG = "WaterWatch/Wear"
    }
}

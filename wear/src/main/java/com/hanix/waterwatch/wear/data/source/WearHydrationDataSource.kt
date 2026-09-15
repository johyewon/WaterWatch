package com.hanix.waterwatch.wear.data.source

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.hanix.waterwatch.common.KEY_EPOCH_DAY
import com.hanix.waterwatch.common.KEY_TOTAL_ML
import com.hanix.waterwatch.common.LOG_RESULT_OK
import com.hanix.waterwatch.common.PATH_LOG_HYDRATION
import com.hanix.waterwatch.common.PATH_LOG_RESULT
import com.hanix.waterwatch.common.PATH_TODAY_TOTAL
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/** 폰과의 수분 섭취 데이터 송수신 */
interface WearHydrationDataSource {

    /** 폰이 발행한 오늘 총량. 오늘 것이 아닌 값은 흘리지 않는다. */
    fun todayTotalMl(): Flow<Int>

    /** 폰이 회신한 기록 성공 여부 */
    fun recordResults(): Flow<Boolean>

    suspend fun requestRecord(ml: Int)
}

internal class WearHydrationDataSourceImpl(context: Context) : WearHydrationDataSource {

    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    /*
     * 앱이 떠 있는 동안만 듣는다. 타일·컴플리케이션이 붙으면 WearableListenerService 로 옮겨야 한다.
     * 리스너를 붙이기 전에 싱크돼 있던 값은 이벤트로 오지 않으므로 최초 1회 직접 읽는다.
     */
    override fun todayTotalMl(): Flow<Int> = callbackFlow {
        val listener = DataClient.OnDataChangedListener { events ->
            events.filter { it.type == DataEvent.TYPE_CHANGED }
                .mapNotNull { it.dataItem.todayTotalMlOrNull() }
                .forEach { trySend(it) }
        }

        // Play services 미준비·미연결이면 Task 가 실패한다. 구독이 통째로 죽지 않도록 삼킨다.
        runCatching {
            dataClient.addListener(listener).await()
            cachedTodayTotalMl().forEach { trySend(it) }
        }.onFailure { Log.w(TAG, "총량 구독 실패", it) }

        awaitClose { dataClient.removeListener(listener) }
    }

    override fun recordResults(): Flow<Boolean> = callbackFlow {
        val listener = MessageClient.OnMessageReceivedListener { event ->
            if (event.path == PATH_LOG_RESULT) trySend(String(event.data) == LOG_RESULT_OK)
        }

        runCatching { messageClient.addListener(listener).await() }
            .onFailure { Log.w(TAG, "기록 결과 구독 실패", it) }

        awaitClose { messageClient.removeListener(listener) }
    }

    /*
     * MessageClient 는 버퍼링하지 않아 폰이 연결돼 있어야만 전달된다.
     *
     * ponytail: 페어링 대상이 폰 하나라 연결된 노드 전체에 보낸다. 기기가 늘면 CapabilityClient 로 대상 지정.
     */
    override suspend fun requestRecord(ml: Int) {
        val nodes = nodeClient.connectedNodes.await()
        check(nodes.isNotEmpty()) { "연결된 기기가 없음" }

        nodes.forEach {
            messageClient.sendMessage(it.id, PATH_LOG_HYDRATION, ml.toString().toByteArray()).await()
        }
    }

    private suspend fun cachedTodayTotalMl(): List<Int> {
        val uri = Uri.Builder()
            .scheme(PutDataRequest.WEAR_URI_SCHEME)
            .path(PATH_TODAY_TOTAL)
            .build()

        val items = dataClient.getDataItems(uri).await()
        return try {
            items.mapNotNull { it.todayTotalMlOrNull() }
        } finally {
            items.release()
        }
    }

    /** DataItem 은 폰이 다시 발행할 때까지 남아 있어서, 날짜를 안 보면 어제 총량을 오늘 값으로 표시하게 된다. */
    private fun DataItem.todayTotalMlOrNull(): Int? {
        if (uri.path != PATH_TODAY_TOTAL) return null

        val dataMap = DataMapItem.fromDataItem(this).dataMap
        val epochDay = dataMap.getLong(KEY_EPOCH_DAY)
        if (epochDay != LocalDate.now().toEpochDay()) {
            Log.i(TAG, "오늘 값이 아니라 무시 — epochDay=$epochDay")
            return null
        }
        return dataMap.getInt(KEY_TOTAL_ML)
    }

    companion object {
        private const val TAG = "WaterWatch/Wear"
    }
}

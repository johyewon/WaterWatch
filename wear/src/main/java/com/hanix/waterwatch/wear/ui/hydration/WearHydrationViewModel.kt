package com.hanix.waterwatch.wear.ui.hydration

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanix.waterwatch.wear.data.source.TodayIntake
import com.hanix.waterwatch.wear.data.source.WearHydrationDataSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WearHydrationViewModel(
    private val wearHydrationDataSource: WearHydrationDataSource
) : ViewModel() {

    /** null 은 폰에서 아직 오늘 값을 받지 못한 상태 */
    val todayIntake: StateFlow<TodayIntake?> = wearHydrationDataSource.todayIntake()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = null
        )

    /** 메시지 전달 성공 여부. 폰에서의 기록 성공까지는 보장하지 않는다. */
    private val _sendResult = MutableStateFlow<Result<Unit>?>(null)
    val sendResult: StateFlow<Result<Unit>?> = _sendResult.asStateFlow()

    /**
     * 보낸 직후 띄우는 안내 문구. null 이면 숨긴다.
     * 화면 숫자는 폰을 한 바퀴 돌아 1초 넘게 걸려 바뀌므로, 눌린 건 이걸로 먼저 알린다.
     */
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var sendJob: Job? = null
    private var toastDismissJob: Job? = null

    /** 폰이 회신한 기록 성공 여부. null 은 아직 회신 없음 */
    private val _recordAccepted = MutableStateFlow<Boolean?>(null)
    val recordAccepted: StateFlow<Boolean?> = _recordAccepted.asStateFlow()

    init {
        viewModelScope.launch {
            wearHydrationDataSource.recordResults().collect {
                if (!it) Log.w(TAG, "폰에서 기록 실패")
                _recordAccepted.value = it
                // 실패 안내가 뜨는데 '기록했어요' 가 남아 있으면 안 된다.
                if (!it) hideToast()
            }
        }
    }

    fun recordIntake(ml: Int) {
        sendToPhone(label = "기록 ml=$ml", toastMessage = "${ml}ml 기록했어요") {
            // 이전 회신이 남아 있으면 새 요청의 결과로 오해하게 된다.
            _recordAccepted.value = null
            wearHydrationDataSource.requestRecord(ml)
        }
    }

    /** 폰처럼 취소는 토스트를 띄우지 않는다. */
    fun undoLastIntake() {
        sendToPhone(label = "취소", toastMessage = null) { wearHydrationDataSource.requestUndo() }
    }

    /**
     * 한 번에 하나만 보낸다. 그 사이에 들어온 터치는 버린다.
     * 전송 자체는 금방 끝나서 끝난 뒤에도 잠깐 더 막아야 연타가 걸러진다.
     * @param toastMessage 보낸 뒤 띄울 문구. null 이면 떠 있던 토스트도 내린다(방금 취소한 기록을 가리키게 되므로).
     */
    private fun sendToPhone(label: String, toastMessage: String?, request: suspend () -> Unit) {
        if (sendJob?.isActive == true) return
        sendJob = viewModelScope.launch {
            _sendResult.value = runCatching { request() }
                .onSuccess {
                    Log.i(TAG, "전송 성공 $label")
                    if (toastMessage != null) showToast(toastMessage) else hideToast()
                }
                .onFailure { Log.e(TAG, "전송 실패 $label", it) }
            delay(SEND_COOLDOWN_MS)
        }
    }

    /** 연달아 보내면 문구만 바꾸고 사라질 시간을 다시 센다. */
    private fun showToast(message: String) {
        toastDismissJob?.cancel()
        _toastMessage.value = message
        toastDismissJob = viewModelScope.launch {
            delay(TOAST_MS)
            _toastMessage.value = null
        }
    }

    private fun hideToast() {
        toastDismissJob?.cancel()
        _toastMessage.value = null
    }

    companion object {
        private const val TAG = "WaterWatch/Wear"
        private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        private const val SEND_COOLDOWN_MS = 500L
        // 프로토타입 워치 보드 값
        private const val TOAST_MS = 2_500L
    }
}

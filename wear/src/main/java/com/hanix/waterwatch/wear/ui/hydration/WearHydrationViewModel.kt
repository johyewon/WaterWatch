package com.hanix.waterwatch.wear.ui.hydration

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanix.waterwatch.wear.data.source.WearHydrationDataSource
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
    val todayTotalMl: StateFlow<Int?> = wearHydrationDataSource.todayTotalMl()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = null
        )

    /** 메시지 전달 성공 여부. 폰에서의 기록 성공까지는 보장하지 않는다. */
    private val _sendResult = MutableStateFlow<Result<Int>?>(null)
    val sendResult: StateFlow<Result<Int>?> = _sendResult.asStateFlow()

    fun recordIntake(ml: Int) {
        viewModelScope.launch {
            _sendResult.value = runCatching { wearHydrationDataSource.requestRecord(ml); ml }
                .onSuccess { Log.i(TAG, "전송 성공 ml=$it") }
                .onFailure { Log.e(TAG, "전송 실패 ml=$ml", it) }
        }
    }

    companion object {
        private const val TAG = "WaterWatch/Wear"
        private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

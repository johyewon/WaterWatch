package com.hanix.waterwatch.ui.hydration

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanix.waterwatch.data.repo.HydrationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HydrationViewModel(
    private val hydrationRepository: HydrationRepository
) : ViewModel() {

    /** 동기 조회이고 앱 수명 중 바뀌지 않아 상태로 들지 않는다. */
    val isHealthConnectAvailable: Boolean
        get() = hydrationRepository.isHealthConnectAvailable

    // 아래 Boolean? 상태의 null 은 모두 "아직 확인 중"을 뜻한다.
    private val _granted = MutableStateFlow<Boolean?>(null)
    val granted: StateFlow<Boolean?> = _granted.asStateFlow()

    private val _watchConnected = MutableStateFlow<Boolean?>(null)
    val watchConnected: StateFlow<Boolean?> = _watchConnected.asStateFlow()

    /** 바깥 null 은 미조회, 성공 값의 null 은 기록 없음 */
    private val _total = MutableStateFlow<Result<Double?>?>(null)
    val total: StateFlow<Result<Double?>?> = _total.asStateFlow()

    init {
        loadWatchConnection()
        loadPermissionState()
    }

    /** 권한 요청은 ActivityResultContract 라서 화면이 수행하고 결과만 넘겨준다. */
    fun onPermissionResult(granted: Boolean) {
        Log.i(TAG, "요청 결과 granted=$granted")
        _granted.value = granted
        if (granted) viewModelScope.launch { loadToday() }
    }

    private fun loadWatchConnection() {
        viewModelScope.launch {
            val connected = hydrationRepository.isWatchConnected()
            Log.i(TAG, "워치 연결=$connected")
            _watchConnected.value = connected
        }
    }

    private fun loadPermissionState() {
        viewModelScope.launch {
            Log.i(TAG, "Health Connect 사용 가능=$isHealthConnectAvailable")
            if (isHealthConnectAvailable.not()) return@launch

            val granted = checkGranted { hydrationRepository.hasPermissions() }
            _granted.value = granted
            if (granted) loadToday()
        }
    }

    // ponytail: 확인 실패는 미허용으로 보고 요청까지 진행. 실패 사유별 분기는 필요해지면.
    private suspend fun checkGranted(check: suspend () -> Boolean): Boolean =
        runCatching { check() }
            .onFailure { Log.e(TAG, "권한 확인 실패", it) }
            .getOrDefault(false)
            .also { Log.i(TAG, "권한 확인 granted=$it") }

    private suspend fun loadToday() {
        _total.value = runCatching { hydrationRepository.todayTotalMl() }
            .onSuccess { Log.i(TAG, "조회 성공 — 오늘 총량=${it?.let { ml -> "$ml ml" } ?: "기록 없음"}") }
            .onFailure { Log.e(TAG, "조회 실패", it) }
    }

    companion object {
        private const val TAG = "WaterWatch/HC"
    }
}

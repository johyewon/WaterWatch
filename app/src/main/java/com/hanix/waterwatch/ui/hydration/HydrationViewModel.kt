package com.hanix.waterwatch.ui.hydration

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanix.waterwatch.data.repo.HydrationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HydrationViewModel(
    private val hydrationRepository: HydrationRepository
) : ViewModel() {

    /** 동기 조회이고 앱 수명 중 바뀌지 않아 상태로 들지 않는다. */
    val isHealthConnectAvailable: Boolean
        get() = hydrationRepository.isHealthConnectAvailable

    /** null 은 아직 확인 중 */
    private val _granted = MutableStateFlow<Boolean?>(null)
    val granted: StateFlow<Boolean?> = _granted.asStateFlow()

    /** 바깥 null 은 미조회, 성공 값의 null 은 기록 없음 */
    private val _total = MutableStateFlow<Result<Double?>?>(null)
    val total: StateFlow<Result<Double?>?> = _total.asStateFlow()

    /** null 은 DataStore 첫 값을 아직 못 읽은 상태 */
    val goalMl: StateFlow<Int?> = hydrationRepository.goalMl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /*
     * 권한 시트 띄우기는 상태가 아니라 일회성 액션이다. StateFlow 로 두면 화면이 띄운 뒤
     * 수동으로 꺼줘야 하고, Activity 재생성마다 다시 뜬다.
     * Channel 은 수집 전에 보낸 값도 버퍼에 담아 두고, 다시 수집해도 같은 값을 또 주지 않는다.
     */
    private val _requestPermissions = Channel<Unit>(Channel.BUFFERED)
    val requestPermissions: Flow<Unit> = _requestPermissions.receiveAsFlow()

    /** 방금 기록한 양(ml). 토스트로 잠깐 보여주고, null 이면 숨긴다. */
    private val _recordedToastMl = MutableStateFlow<Double?>(null)
    val recordedToastMl: StateFlow<Double?> = _recordedToastMl.asStateFlow()

    private var intakeChangeJob: Job? = null
    private var toastDismissJob: Job? = null

    init {
        loadPermissionState()
    }

    /** 권한 요청은 ActivityResultContract 라서 화면이 수행하고 결과만 넘겨준다. */
    fun onPermissionResult(granted: Boolean) {
        Log.i(TAG, "요청 결과 granted=$granted")
        _granted.value = granted
        if (granted) viewModelScope.launch { loadToday() }
    }

    /** 기록 후 총량을 다시 읽는다. 조회 경로에 워치 발행이 물려 있어 워치까지 같이 갱신된다. */
    fun recordIntake(ml: Double) {
        launchIntakeChange {
            runCatching { hydrationRepository.recordIntake(ml) }
                .onSuccess {
                    Log.i(TAG, "기록 성공 ml=$ml")
                    showRecordedToast(ml)
                    loadToday()
                }
                .onFailure { Log.e(TAG, "기록 실패 ml=$ml", it) }
        }
    }

    /** 기록과 같은 이유로 취소 후에도 총량을 다시 읽어 워치까지 갱신한다. */
    fun undoLastIntake() {
        launchIntakeChange {
            runCatching { hydrationRepository.undoLastIntake() }
                .onSuccess { deleted ->
                    Log.i(TAG, "기록 취소 deleted=$deleted")
                    if (deleted) {
                        // 방금 기록 토스트가 떠 있으면 취소한 기록을 가리키게 되므로 바로 내린다.
                        hideRecordedToast()
                        loadToday()
                    }
                }
                .onFailure { Log.e(TAG, "기록 취소 실패", it) }
        }
    }

    /**
     * 기록·취소는 한 번에 하나만 한다. 그 사이에 들어온 터치는 버린다.
     * 처리 자체는 수십 ms 라 끝난 뒤에도 잠깐 더 막아야 연타가 걸러진다.
     */
    private fun launchIntakeChange(block: suspend () -> Unit) {
        if (intakeChangeJob?.isActive == true) {
            Log.i(TAG, "처리 중이라 터치 무시")
            return
        }
        intakeChangeJob = viewModelScope.launch {
            block()
            delay(INTAKE_CHANGE_COOLDOWN_MS)
        }
    }

    /** 연달아 기록하면 내용만 바꾸고 사라질 시간을 다시 센다. */
    private fun showRecordedToast(ml: Double) {
        toastDismissJob?.cancel()
        _recordedToastMl.value = ml
        toastDismissJob = viewModelScope.launch {
            delay(RECORDED_TOAST_MS)
            _recordedToastMl.value = null
        }
    }

    private fun hideRecordedToast() {
        toastDismissJob?.cancel()
        _recordedToastMl.value = null
    }

    private fun loadPermissionState() {
        viewModelScope.launch {
            Log.i(TAG, "Health Connect 사용 가능=$isHealthConnectAvailable")
            if (isHealthConnectAvailable.not()) return@launch

            val granted = checkGranted("필수") { hydrationRepository.hasPermissions() }
            _granted.value = granted
            if (granted) loadToday()

            // 필수 권한만 허용된 상태에서도 요청해야 백그라운드 읽기가 허용될 기회가 생긴다.
            if (checkGranted("전체") { hydrationRepository.hasAllRequestedPermissions() }.not()) {
                _requestPermissions.send(Unit)
            }
        }
    }

    // ponytail: 확인 실패는 미허용으로 보고 요청까지 진행. 실패 사유별 분기는 필요해지면.
    private suspend fun checkGranted(label: String, check: suspend () -> Boolean): Boolean =
        runCatching { check() }
            .onFailure { Log.e(TAG, "$label 권한 확인 실패", it) }
            .getOrDefault(false)
            .also { Log.i(TAG, "$label 권한 확인 granted=$it") }

    private suspend fun loadToday() {
        _total.value = runCatching { hydrationRepository.todayTotalMl() }
            .onSuccess { Log.i(TAG, "조회 성공 — 오늘 총량=${it?.let { ml -> "$ml ml" } ?: "기록 없음"}") }
            .onFailure { Log.e(TAG, "조회 실패", it) }
    }

    companion object {
        private const val TAG = "WaterWatch/HC"
        private const val INTAKE_CHANGE_COOLDOWN_MS = 500L
        private const val RECORDED_TOAST_MS = 4_000L
    }
}

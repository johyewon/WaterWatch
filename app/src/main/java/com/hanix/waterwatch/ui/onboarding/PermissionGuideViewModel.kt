package com.hanix.waterwatch.ui.onboarding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanix.waterwatch.data.repo.HydrationRepository
import com.hanix.waterwatch.data.source.OnboardingDataSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** 거부한 뒤 띄우는 안내 팝업의 종류 */
enum class PermissionDeniedKind {
    /** 1번째 거부. 권한 화면을 한 번 더 띄울 수 있다. */
    Reask,

    /** 2번째 거부부터. Health Connect 가 권한 화면을 더 띄우지 않아 설정에서 켜야 한다. */
    Settings
}

/** 화면이 해야 하는 일회성 동작. 권한 화면·설정 화면은 ActivityResult 라서 화면이 띄운다. */
enum class PermissionGuideEvent { RequestPermissions, RequestBackgroundPermission, OpenSettings, Granted }

class PermissionGuideViewModel(
    private val hydrationRepository: HydrationRepository,
    private val onboardingDataSource: OnboardingDataSource
) : ViewModel() {

    private val _dialog = MutableStateFlow<PermissionDeniedKind?>(null)
    val dialog: StateFlow<PermissionDeniedKind?> = _dialog.asStateFlow()

    private val _events = Channel<PermissionGuideEvent>(Channel.BUFFERED)
    val events: Flow<PermissionGuideEvent> = _events.receiveAsFlow()

    /** 한 번이라도 거부했으면 권한 화면 대신 안내 팝업부터 다시 보여준다. */
    fun onStartClick() {
        viewModelScope.launch {
            // 설정에서 직접 켜고 들어온 경우
            if (hasPermissions()) {
                finishWithBackgroundPermission()
                return@launch
            }
            val denials = onboardingDataSource.permissionDenialCount()
            if (denials == 0) {
                _events.send(PermissionGuideEvent.RequestPermissions)
            } else {
                _dialog.value = dialogFor(denials)
            }
        }
    }

    /** 이 요청에 백그라운드 읽기도 들어 있어서, 필수 권한만 받았어도 다시 묻지 않고 넘어간다. */
    fun onPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            if (granted) {
                _events.send(PermissionGuideEvent.Granted)
                return@launch
            }
            val denials = onboardingDataSource.addPermissionDenial()
            Log.i(TAG, "권한 거부 누적=$denials")
            _dialog.value = dialogFor(denials)
        }
    }

    fun onDialogActionClick() {
        val event = when (_dialog.value ?: return) {
            PermissionDeniedKind.Reask -> PermissionGuideEvent.RequestPermissions
            PermissionDeniedKind.Settings -> PermissionGuideEvent.OpenSettings
        }
        _dialog.value = null
        viewModelScope.launch { _events.send(event) }
    }

    fun onDialogDismiss() {
        _dialog.value = null
    }

    /** 설정에서 돌아오면 다시 확인한다. 여전히 꺼져 있으면 같은 안내를 다시 띄운다. */
    fun onSettingsReturned() {
        viewModelScope.launch {
            val granted = hasPermissions()
            Log.i(TAG, "설정에서 돌아옴 granted=$granted")
            if (granted) {
                finishWithBackgroundPermission()
            } else {
                _dialog.value = PermissionDeniedKind.Settings
            }
        }
    }

    /** 허용 여부와 상관없이 넘어간다. 없어도 폰 화면은 동작하고, 워치 기록이 폰 앱을 열 때 반영될 뿐이다. */
    fun onBackgroundPermissionResult(granted: Boolean) {
        Log.i(TAG, "백그라운드 읽기 요청 결과 granted=$granted")
        viewModelScope.launch { _events.send(PermissionGuideEvent.Granted) }
    }

    /**
     * 설정에서 켜면 백그라운드 읽기는 따로 켜야 해서 빠지기 쉽다. 빠졌으면 여기서 한 번만 묻는다.
     * 워치에서 기록할 때 폰 앱이 안 떠 있어도 총량을 다시 집계하는 데 필요하다.
     */
    private suspend fun finishWithBackgroundPermission() {
        val hasBackground = runCatching { hydrationRepository.hasAllRequestedPermissions() }
            .onFailure { Log.e(TAG, "백그라운드 읽기 권한 확인 실패", it) }
            .getOrDefault(false)
        _events.send(
            if (hasBackground) PermissionGuideEvent.Granted else PermissionGuideEvent.RequestBackgroundPermission
        )
    }

    // ponytail: 확인 실패는 미허용으로 본다. HydrationViewModel 과 같은 기준.
    private suspend fun hasPermissions(): Boolean =
        runCatching { hydrationRepository.hasPermissions() }
            .onFailure { Log.e(TAG, "권한 확인 실패", it) }
            .getOrDefault(false)

    private fun dialogFor(denials: Int): PermissionDeniedKind =
        if (denials >= DENIALS_UNTIL_SETTINGS) PermissionDeniedKind.Settings else PermissionDeniedKind.Reask

    companion object {
        private const val TAG = "WaterWatch/Onboarding"

        /** Android 11+ 에서 두 번 거부하면 다시 묻지 않는다. Health Connect 권한도 같다. */
        private const val DENIALS_UNTIL_SETTINGS = 2
    }
}

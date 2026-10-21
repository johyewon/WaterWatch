package com.hanix.waterwatch.data.source

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/** 첫 진입 안내(온보딩) 진행 상태 */
interface OnboardingDataSource {

    val isCompleted: Flow<Boolean>

    suspend fun complete()

    /**
     * Health Connect 권한을 거부한 횟수
     *
     * Health Connect 에는 다시 물어볼 수 있는지 알려주는 API 가 없고, 두 번 거부하면 권한 화면을 더 띄우지 않는다.
     * 그래서 앱이 직접 센다. 앱을 지우면 Health Connect 쪽 거부 기록과 함께 초기화된다.
     */
    suspend fun permissionDenialCount(): Int

    /** @return 더한 뒤의 거부 횟수 */
    suspend fun addPermissionDenial(): Int
}

// 같은 파일에 DataStore 인스턴스가 둘 이상 생기면 런타임 예외라 top-level 위임으로 하나만 둔다.
private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding")

internal class OnboardingDataSourceImpl(private val context: Context) : OnboardingDataSource {

    // 읽기 실패로 앱 진입이 막히지 않도록 빈 값(안내를 안 끝냄, 거부 0번)으로 본다.
    private val preferences: Flow<Preferences> = context.onboardingDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    override val isCompleted: Flow<Boolean> = preferences.map { it[KEY_COMPLETED] ?: false }

    override suspend fun complete() {
        context.onboardingDataStore.edit { it[KEY_COMPLETED] = true }
    }

    override suspend fun permissionDenialCount(): Int =
        preferences.first()[KEY_PERMISSION_DENIALS] ?: 0

    override suspend fun addPermissionDenial(): Int =
        context.onboardingDataStore.edit {
            it[KEY_PERMISSION_DENIALS] = (it[KEY_PERMISSION_DENIALS] ?: 0) + 1
        }[KEY_PERMISSION_DENIALS] ?: 0

    companion object {
        private val KEY_COMPLETED: Preferences.Key<Boolean> = booleanPreferencesKey("completed")
        private val KEY_PERMISSION_DENIALS: Preferences.Key<Int> = intPreferencesKey("permission_denials")
    }
}

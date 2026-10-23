package com.hanix.waterwatch.data.source

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** 첫 진입 안내(온보딩)를 끝냈는지 */
interface OnboardingDataSource {

    val isCompleted: Flow<Boolean>

    suspend fun complete()
}

// 같은 파일에 DataStore 인스턴스가 둘 이상 생기면 런타임 예외라 top-level 위임으로 하나만 둔다.
private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding")

internal class OnboardingDataSourceImpl(private val context: Context) : OnboardingDataSource {

    // 읽기 실패로 앱 진입이 막히지 않도록 안 끝낸 것으로 보고 안내부터 다시 보여준다.
    override val isCompleted: Flow<Boolean> = context.onboardingDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_COMPLETED] ?: false }

    override suspend fun complete() {
        context.onboardingDataStore.edit { it[KEY_COMPLETED] = true }
    }

    companion object {
        private val KEY_COMPLETED: Preferences.Key<Boolean> = booleanPreferencesKey("completed")
    }
}

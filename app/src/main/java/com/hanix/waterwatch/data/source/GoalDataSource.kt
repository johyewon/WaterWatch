package com.hanix.waterwatch.data.source

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** 하루 수분 섭취 목표 */
interface GoalDataSource {

    /** 저장된 값이 없으면 기본 목표를 준다. */
    val goalMl: Flow<Int>
}

// 같은 파일에 DataStore 인스턴스가 둘 이상 생기면 런타임 예외라 top-level 위임으로 하나만 둔다.
private val Context.goalDataStore by preferencesDataStore(name = "goal")

internal class GoalDataSourceImpl(private val context: Context) : GoalDataSource {

    // 읽기 실패로 화면 전체가 죽지 않도록 기본 목표로 대체한다.
    override val goalMl: Flow<Int> = context.goalDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_GOAL_ML] ?: DEFAULT_GOAL_ML }

    companion object {
        private val KEY_GOAL_ML: Preferences.Key<Int> = intPreferencesKey("goal_ml")
        private const val DEFAULT_GOAL_ML = 2_000
    }
}

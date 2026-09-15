package com.hanix.waterwatch.locator

import android.annotation.SuppressLint
import android.content.Context
import com.hanix.waterwatch.data.repo.HydrationRepository
import com.hanix.waterwatch.data.source.HealthConnectDataSource
import com.hanix.waterwatch.data.source.HealthConnectDataSourceImpl
import com.hanix.waterwatch.data.source.WearSyncDataSource
import com.hanix.waterwatch.data.source.WearSyncDataSourceImpl

/**
 * 앱 범위 의존성 제공
 *
 * 화면과 서비스가 같은 그래프를 각각 조립하지 않도록 배선을 한곳에 모은다.
 */
@SuppressLint("StaticFieldLeak")
internal object ServiceLocator {

    private lateinit var context: Context

    /**
     * application context 주입
     *
     * Application.onCreate 에서 한 번 호출한다. 두 번째 호출은 무시된다.
     *
     * @param context application context 를 꺼낼 Context
     */
    fun applicationContext(context: Context) {
        if (::context.isInitialized.not()) {
            this.context = context.applicationContext
        }
    }

    // Create Singleton Provider
    private val healthConnectDataSource: HealthConnectDataSource by lazy {
        HealthConnectDataSourceImpl(context)
    }

    private val wearSyncDataSource: WearSyncDataSource by lazy {
        WearSyncDataSourceImpl(context)
    }

    val hydrationRepository: HydrationRepository by lazy {
        HydrationRepository(
            healthConnectDataSource = healthConnectDataSource,
            wearSyncDataSource = wearSyncDataSource,
        )
    }
    // End Singleton Provider
}

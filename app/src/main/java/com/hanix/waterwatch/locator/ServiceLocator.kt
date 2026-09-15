package com.hanix.waterwatch.locator

import android.annotation.SuppressLint
import android.content.Context
import com.hanix.waterwatch.data.repo.HydrationRepository
import com.hanix.waterwatch.data.source.HealthConnectDataSource
import com.hanix.waterwatch.data.source.HealthConnectDataSourceImpl

/** 화면이 구체 구현을 알지 않도록 배선을 한곳에 모은다. */
@SuppressLint("StaticFieldLeak")
internal object ServiceLocator {

    private lateinit var context: Context

    /** Application.onCreate 에서 한 번 호출한다. 두 번째 호출은 무시된다. */
    fun applicationContext(context: Context) {
        if (::context.isInitialized.not()) {
            this.context = context.applicationContext
        }
    }

    // Create Singleton Provider
    private val healthConnectDataSource: HealthConnectDataSource by lazy {
        HealthConnectDataSourceImpl(context)
    }

    val hydrationRepository: HydrationRepository by lazy {
        HydrationRepository(healthConnectDataSource = healthConnectDataSource)
    }
    // End Singleton Provider
}

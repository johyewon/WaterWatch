package com.hanix.waterwatch

import android.app.Application
import com.hanix.waterwatch.locator.ServiceLocator

class WaterWatchApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.applicationContext(this)
    }
}

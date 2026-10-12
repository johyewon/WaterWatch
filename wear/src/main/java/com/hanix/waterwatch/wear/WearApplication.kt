package com.hanix.waterwatch.wear

import android.app.Application
import com.hanix.waterwatch.wear.locator.ServiceLocator

/** Activity 보다 먼저 생성되므로, 화면이 뜰 때는 그래프가 준비돼 있다. */
class WearApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.applicationContext(this)
    }
}

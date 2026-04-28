package com.wifimapper

import android.app.Application
import com.wifimapper.di.initKoin

class WiFiMapperApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(this)
    }
}

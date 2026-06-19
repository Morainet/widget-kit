package com.morainet.widget.sample

import android.app.Application
import com.morainet.widget.sample.widget.WeatherRefreshWorker
import com.morainet.widget.workmanager.WidgetScheduler

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WeatherRefreshWorker.schedule(this)
        WidgetScheduler.scheduleOnNetworkAvailable<com.morainet.widget.sample.widget.WeatherNetworkRefreshWorker>(
            context = this,
            uniqueWorkName = "weather_widget_network_refresh",
        )
    }
}

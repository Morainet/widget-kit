package net.morainet.widget.sample

import android.app.Application
import net.morainet.widget.sample.widget.WeatherRefreshWorker
import net.morainet.widget.workmanager.WidgetScheduler

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WeatherRefreshWorker.schedule(this)
        WidgetScheduler.scheduleOnNetworkAvailable<net.morainet.widget.sample.widget.WeatherNetworkRefreshWorker>(
            context = this,
            uniqueWorkName = "weather_widget_network_refresh",
        )
    }
}

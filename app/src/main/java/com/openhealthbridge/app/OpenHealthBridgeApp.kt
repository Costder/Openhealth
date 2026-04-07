package com.openhealthbridge.app

import android.app.Application
import com.openhealthbridge.app.di.AppModule
import com.openhealthbridge.data.sync.DailySyncWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class OpenHealthBridgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppModule.from(this).localApiServer.start()
        val request = PeriodicWorkRequestBuilder<DailySyncWorker>(24, TimeUnit.HOURS)
            .setConstraints(DailySyncWorker.requiredConstraints())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily-ohc-sync",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    override fun onTerminate() {
        AppModule.from(this).localApiServer.stop()
        super.onTerminate()
    }
}

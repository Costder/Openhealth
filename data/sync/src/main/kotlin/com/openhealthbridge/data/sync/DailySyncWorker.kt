package com.openhealthbridge.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters

class DailySyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            SyncRuntimeFactory.get(applicationContext).coordinator.runSync()
            Result.success()
        } catch (error: Throwable) {
            Result.failure()
        }
    }

    companion object {
        fun requiredConstraints(): Constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
    }
}

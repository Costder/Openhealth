package com.openhealthbridge.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters

class DailySyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // MVP scaffold: sync + dirty flag orchestration implemented in milestone 2.
        return Result.success()
    }

    companion object {
        fun requiredConstraints(): Constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
    }
}

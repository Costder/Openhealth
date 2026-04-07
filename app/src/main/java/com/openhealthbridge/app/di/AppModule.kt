package com.openhealthbridge.app.di

import android.content.Context
import com.openhealthbridge.core.common.Constants
import com.openhealthbridge.data.sync.SyncRuntime
import com.openhealthbridge.data.sync.SyncRuntimeFactory
import com.openhealthbridge.integration.api.LocalApiServer

data class AppServices(
    val syncRuntime: SyncRuntime,
    val localApiServer: LocalApiServer
)

object AppModule {
    @Volatile
    private var services: AppServices? = null

    fun from(context: Context): AppServices {
        return services ?: synchronized(this) {
            services ?: build(context.applicationContext).also { services = it }
        }
    }

    private fun build(context: Context): AppServices {
        val syncRuntime = SyncRuntimeFactory.get(context)
        return AppServices(
            syncRuntime = syncRuntime,
            localApiServer = LocalApiServer(syncRuntime.service, Constants.DEFAULT_API_PORT)
        )
    }
}

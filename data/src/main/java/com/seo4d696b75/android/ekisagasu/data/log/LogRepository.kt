package com.seo4d696b75.android.ekisagasu.data.log

import com.seo4d696b75.android.ekisagasu.data.database.AppLog
import com.seo4d696b75.android.ekisagasu.data.database.AppRebootLog
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    suspend fun write(type: AppLogType, message: String, isError: Boolean)

    val history: Flow<List<AppRebootLog>>
    val filter: Flow<LogTarget>

    suspend fun filterLogSince(since: AppRebootLog)

    val logs: Flow<List<AppLog>>

    suspend fun onAppBoot()

    suspend fun onAppFinish()
}

package jp.seo.station.ekisagasu.repository

import android.content.Context
import jp.seo.station.ekisagasu.database.AppLog
import jp.seo.station.ekisagasu.database.AppRebootLog
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.model.LogTarget
import kotlinx.coroutines.flow.StateFlow

interface LogRepository {
    suspend fun saveMessage(message: AppMessage)
    val history: StateFlow<List<AppRebootLog>>
    val logFilter: StateFlow<LogTarget>
    suspend fun filterLogSince(since: AppRebootLog)
    val logs: StateFlow<List<AppLog>>
    suspend fun onAppBoot(context: Context)
    suspend fun onAppFinish(context: Context)
}

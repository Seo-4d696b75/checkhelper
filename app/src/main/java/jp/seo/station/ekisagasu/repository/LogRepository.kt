package jp.seo.station.ekisagasu.repository

import android.content.Context
import jp.seo.station.ekisagasu.database.AppLog
import jp.seo.station.ekisagasu.database.AppRebootLog
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.model.LogTarget
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    suspend fun saveMessage(message: AppMessage)
    val history: Flow<List<AppRebootLog>>
    val logFilter: Flow<LogTarget>
    suspend fun filterLogSince(since: AppRebootLog)
    val logs: Flow<List<AppLog>>
    suspend fun onAppBoot(context: Context)
    suspend fun onAppFinish(context: Context)
}

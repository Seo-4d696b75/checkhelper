package com.seo4d696b75.android.ekisagasu.domain.log

import kotlinx.coroutines.flow.Flow

interface LogRepository {
    suspend fun write(type: AppLogType, message: String, isError: Boolean)

    val history: Flow<List<AppLogTarget>>
    val target: Flow<AppLogTarget>

    fun setTarget(target: AppLogTarget)

    val logs: Flow<List<AppLog>>

    suspend fun onAppBoot()

    suspend fun onAppFinish()
}

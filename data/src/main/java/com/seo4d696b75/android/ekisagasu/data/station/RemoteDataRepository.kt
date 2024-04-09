package com.seo4d696b75.android.ekisagasu.data.station

import java.io.File

interface RemoteDataRepository {
    suspend fun getLatestDataVersion(cache: Boolean = false): LatestDataVersion

    suspend fun download(
        version: Long,
        dir: File,
        callback: (size: Long) -> Unit,
    )
}

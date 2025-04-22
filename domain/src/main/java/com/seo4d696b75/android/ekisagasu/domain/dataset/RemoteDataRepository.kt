package com.seo4d696b75.android.ekisagasu.domain.dataset

import com.seo4d696b75.android.ekisagasu.domain.cache.MemoryCache
import java.io.File

interface RemoteDataRepository {
    val latestDataVersion: MemoryCache<LatestDataVersion>

    suspend fun download(
        version: Long,
        dir: File,
        callback: (size: Long) -> Unit,
    )
}

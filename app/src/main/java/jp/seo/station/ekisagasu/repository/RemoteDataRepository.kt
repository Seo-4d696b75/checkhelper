package jp.seo.station.ekisagasu.repository

import jp.seo.station.ekisagasu.model.DataLatestInfo
import java.io.File

interface RemoteDataRepository {
    suspend fun getLatestDataVersion(cache: Boolean = false): DataLatestInfo

    suspend fun download(
        version: Long,
        dir: File,
        callback: (size: Long) -> Unit,
    )
}

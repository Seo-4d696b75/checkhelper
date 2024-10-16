package com.seo4d696b75.android.ekisagasu.data.station

import com.seo4d696b75.android.ekisagasu.data.api.StationDataService
import com.seo4d696b75.android.ekisagasu.data.file.unzip
import com.seo4d696b75.android.ekisagasu.domain.dataset.LatestDataVersion
import com.seo4d696b75.android.ekisagasu.domain.dataset.RemoteDataRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

class RemoteDataRepositoryImpl @Inject constructor(
    private val api: StationDataService,
) : RemoteDataRepository {
    // cached version
    private var _lastCheckedVersion: LatestDataVersion? = null

    override suspend fun getLatestDataVersion(cache: Boolean) = withContext(Dispatchers.IO) {
        val last = _lastCheckedVersion
        if (last != null && cache) {
            last
        } else {
            val info = api.getLatestInfo()
            _lastCheckedVersion = info
            info
        }
    }

    override suspend fun download(
        version: Long,
        dir: File,
        callback: (size: Long) -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        callback(0L)
        val file = File(dir, "json.zip")
        api.getLatestData(version).also { res ->
            res.byteStream().use { inputStream ->
                file.outputStream().use { outputStream ->
                    var bytes = 0L
                    val buf = ByteArray(8192)
                    while (true) {
                        val read = inputStream.read(buf)
                        if (read < 0) {
                            break
                        }
                        bytes += read
                        outputStream.write(buf, 0, read)
                        callback(bytes)
                    }
                }
            }
        }
        unzip(file, dir)
    }
}

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface RemoteDataRepositoryModule {
    @Binds
    @Singleton
    fun bindRemoteDataRepository(impl: RemoteDataRepositoryImpl): RemoteDataRepository
}

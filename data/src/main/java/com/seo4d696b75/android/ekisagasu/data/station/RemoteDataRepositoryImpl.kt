package com.seo4d696b75.android.ekisagasu.data.station

import com.seo4d696b75.android.ekisagasu.data.api.StationDataService
import com.seo4d696b75.android.ekisagasu.data.cache.MemoryCacheStore
import com.seo4d696b75.android.ekisagasu.data.cache.cacheOf
import com.seo4d696b75.android.ekisagasu.data.file.unzip
import com.seo4d696b75.android.ekisagasu.domain.dataset.RemoteDataRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

internal class RemoteDataRepositoryImpl @Inject constructor(
    store: MemoryCacheStore,
    private val api: StationDataService,
) : RemoteDataRepository {

    override val latestDataVersion = store.cacheOf {
        api.getLatestInfo()
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
@InstallIn(ActivityRetainedComponent::class)
internal interface RemoteDataRepositoryModule {
    @Binds
    fun bindRemoteDataRepository(impl: RemoteDataRepositoryImpl): RemoteDataRepository
}

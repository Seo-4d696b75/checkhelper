package com.seo4d696b75.android.ekisagasu.data.station

import com.seo4d696b75.android.ekisagasu.data.database.DataVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class DataUpdateUseCase @Inject constructor(
    private val repository: DataRepository,
    private val remoteRepository: RemoteDataRepository,
) {
    private val _progress = MutableStateFlow<DataUpdateProgress>(DataUpdateProgress.Download(0))
    val progress = _progress.asStateFlow()

    suspend operator fun invoke(
        info: LatestDataVersion,
        dir: File,
    ): Result<DataVersion> = withContext(Dispatchers.IO) {
        if (!dir.exists() || !dir.isDirectory) {
            require(dir.mkdir())
        }
        _progress.value = DataUpdateProgress.Download(0)
        runCatching {
            var percent = 0
            remoteRepository.download(info.version, dir) {
                val p = (it * 100 / info.length).toInt()
                if (p in 1..100 && p > percent) {
                    _progress.value = DataUpdateProgress.Download(p)
                    percent = p
                }
            }
            _progress.value = DataUpdateProgress.Save
            repository.updateData(info, dir)
        }.onFailure {
            Timber.w(it)
        }.also {
            dir.deleteRecursively()
        }
    }
}

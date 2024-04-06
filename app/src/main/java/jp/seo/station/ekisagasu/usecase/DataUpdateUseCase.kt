package jp.seo.station.ekisagasu.usecase

import jp.seo.station.ekisagasu.database.DataVersion
import jp.seo.station.ekisagasu.model.DataLatestInfo
import jp.seo.station.ekisagasu.model.DataUpdateProgress
import jp.seo.station.ekisagasu.repository.DataRepository
import jp.seo.station.ekisagasu.repository.RemoteDataRepository
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

    suspend operator fun invoke(info: DataLatestInfo, dir: File): Result<DataVersion> =
        withContext(Dispatchers.IO) {
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

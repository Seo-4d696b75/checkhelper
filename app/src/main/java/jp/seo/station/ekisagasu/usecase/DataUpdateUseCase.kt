package jp.seo.station.ekisagasu.usecase

import jp.seo.station.ekisagasu.core.DataLatestInfo
import jp.seo.station.ekisagasu.core.DataVersion
import jp.seo.station.ekisagasu.core.StationDao
import jp.seo.station.ekisagasu.core.getDownloadClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.floor

class DataUpdateUseCase @Inject constructor(
    private val dao: StationDao,
) {

    private val _progress = MutableSharedFlow<UpdateProgress>()
    val progress: SharedFlow<UpdateProgress> = _progress

    suspend operator fun invoke(info: DataLatestInfo): Result = withContext(Dispatchers.IO) {
        _progress.emit(UpdateProgress.Download(0))
        try {
            var percent = 0
            val download = getDownloadClient { length: Long ->
                val p = floor(length.toFloat() / info.length * 100.0f).toInt()
                if (p in 1..100 && p > percent) {
                    launch { _progress.emit(UpdateProgress.Download(p)) }
                    percent = p
                }
            }
            val data = download.getData(info.url)
            _progress.emit(UpdateProgress.Save)
            if (data.version == info.version) {
                dao.updateData(data)
                val current = dao.getCurrentDataVersion()
                if (info.version == current?.version) {
                    return@withContext Result.Success(current)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Result.Failure
    }

    sealed interface UpdateProgress {
        data class Download(val percent: Int) : UpdateProgress
        object Save : UpdateProgress
    }

    sealed interface Result {
        object Failure : Result
        data class Success(val version: DataVersion) : Result
    }

}

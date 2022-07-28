package jp.seo.station.ekisagasu.usecase

import jp.seo.station.ekisagasu.model.DataLatestInfo
import jp.seo.station.ekisagasu.api.getDownloadClient
import jp.seo.station.ekisagasu.database.StationDao
import jp.seo.station.ekisagasu.model.DataUpdateProgress
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

    private val _progress = MutableSharedFlow<DataUpdateProgress>()
    val progress: SharedFlow<DataUpdateProgress> = _progress

    suspend operator fun invoke(info: DataLatestInfo): DataUpdateResult =
        withContext(Dispatchers.IO) {
            _progress.emit(DataUpdateProgress.Download(0))
            try {
                var percent = 0
                val download = getDownloadClient { length: Long ->
                    val p = floor(length.toFloat() / info.length * 100.0f).toInt()
                    if (p in 1..100 && p > percent) {
                        launch(Dispatchers.Main) { _progress.emit(DataUpdateProgress.Download(p)) }
                        percent = p
                    }
                }
                val data = download.getData(info.url)
                withContext(Dispatchers.Main) {
                    _progress.emit(DataUpdateProgress.Save)
                }
                if (data.version == info.version) {
                    dao.updateData(data)
                    val current = dao.getCurrentDataVersion()
                    if (info.version == current?.version) {
                        return@withContext DataUpdateResult.Success(current)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            DataUpdateResult.Failure
        }

}

package jp.seo.station.ekisagasu.usecase

import jp.seo.station.ekisagasu.api.DownloadClient
import jp.seo.station.ekisagasu.database.StationDao
import jp.seo.station.ekisagasu.model.DataLatestInfo
import jp.seo.station.ekisagasu.model.DataUpdateProgress
import jp.seo.station.ekisagasu.model.StationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@ExperimentalSerializationApi
class DataUpdateUseCase @Inject constructor(
    private val dao: StationDao,
    private val downloadClient: DownloadClient,
    private val json: Json,
) {

    private val _progress = MutableSharedFlow<DataUpdateProgress>()
    val progress: SharedFlow<DataUpdateProgress> = _progress

    suspend operator fun invoke(info: DataLatestInfo): DataUpdateResult =
        withContext(Dispatchers.IO) {
            _progress.emit(DataUpdateProgress.Download(0))
            try {
                var percent = 0
                val data = downloadClient(info.url) {
                    val p = (it * 100 / info.length).toInt()
                    if (p in 1..100 && p > percent) {
                        launch(Dispatchers.Main) {
                            _progress.emit(DataUpdateProgress.Download(p))
                        }
                        percent = p
                    }
                }.let { str ->
                    json.decodeFromString<StationData>(str)
                }
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

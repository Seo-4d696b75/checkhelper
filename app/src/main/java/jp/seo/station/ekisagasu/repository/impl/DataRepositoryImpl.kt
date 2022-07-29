package jp.seo.station.ekisagasu.repository.impl

import jp.seo.station.ekisagasu.api.APIClient
import jp.seo.station.ekisagasu.database.DataVersion
import jp.seo.station.ekisagasu.database.StationDao
import jp.seo.station.ekisagasu.model.DataLatestInfo
import jp.seo.station.ekisagasu.repository.DataRepository
import jp.seo.station.ekisagasu.usecase.DataUpdateResult
import jp.seo.station.ekisagasu.usecase.DataUpdateUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DataRepositoryImpl @Inject constructor(
    private val dao: StationDao,
    private val api: APIClient,
    private val updateUseCase: DataUpdateUseCase,
) : DataRepository {

    override suspend fun getLine(code: Int) = withContext(Dispatchers.IO) {
        dao.getLine(code)
    }

    override suspend fun getLines(codes: List<Int>) = withContext(Dispatchers.IO) {
        dao.getLines(codes)
    }

    override suspend fun getStation(code: Int) = withContext(Dispatchers.IO) {
        dao.getStation(code)
    }

    override suspend fun getStations(codes: List<Int>) = withContext(Dispatchers.IO) {
        dao.getStations(codes)
    }

    private val _currentVersion = MutableStateFlow<DataVersion?>(null)
    private var _dataInitialized: Boolean = false
    private var _lastCheckedVersion: DataLatestInfo? = null

    override val dataInitialized: Boolean
        get() = _dataInitialized

    override val dataVersion: StateFlow<DataVersion?> = _currentVersion

    override val lastCheckedVersion: DataLatestInfo?
        get() = _lastCheckedVersion

    override suspend fun getDataVersion(): DataVersion? = withContext(Dispatchers.IO) {
        val version = dao.getCurrentDataVersion()
        _dataInitialized = version != null
        _currentVersion.value = version
        version
    }

    override suspend fun getLatestDataVersion(forceRefresh: Boolean): DataLatestInfo =
        withContext(Dispatchers.IO) {
            val last = _lastCheckedVersion
            if (last != null && !forceRefresh) {
                last
            } else {
                val info = api.getLatestInfo()
                _lastCheckedVersion = info
                info
            }
        }

    override suspend fun getDataVersionHistory() = dao.getDataVersionHistory()

    override val dataUpdateProgress = updateUseCase.progress

    override suspend fun updateData(info: DataLatestInfo): DataUpdateResult {
        return updateUseCase(info).also {
            when (it) {
                is DataUpdateResult.Success -> {
                    _dataInitialized = true
                    _currentVersion.value = it.version
                }
                is DataUpdateResult.Failure -> {

                }
            }
        }
    }
}
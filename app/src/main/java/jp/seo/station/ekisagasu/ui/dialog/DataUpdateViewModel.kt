package jp.seo.station.ekisagasu.ui.dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.api.DataLatestInfo
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.model.DataUpdateProgress
import jp.seo.station.ekisagasu.repository.AppStateRepository
import jp.seo.station.ekisagasu.repository.DataRepository
import jp.seo.station.ekisagasu.usecase.DataUpdateResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataUpdateViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val appStateRepository: AppStateRepository,
) : ViewModel() {
    val progress = dataRepository.dataUpdateProgress.stateIn(
        viewModelScope, SharingStarted.Eagerly,
        DataUpdateProgress.Download(0),
    )

    private val _result = MutableSharedFlow<DataUpdateResult>()

    val result: SharedFlow<DataUpdateResult> = _result

    private lateinit var info: DataLatestInfo
    private lateinit var type: DataUpdateType

    fun setTargetData(type: DataUpdateType, info: DataLatestInfo) {
        this.info = info
        this.type = type
    }

    fun update() = viewModelScope.launch {
        val result = dataRepository.updateData(info)
        _result.emit(result)
    }

    fun onResult(success: Boolean) = viewModelScope.launch {
        appStateRepository.emitMessage(
            AppMessage.DataUpdateResult(
                type = type,
                success = success,
            )
        )
    }
}

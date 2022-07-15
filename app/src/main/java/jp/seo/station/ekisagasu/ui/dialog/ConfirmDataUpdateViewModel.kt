package jp.seo.station.ekisagasu.ui.dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.api.DataLatestInfo
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.repository.AppStateRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfirmDataUpdateViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository,
) : ViewModel() {

    private lateinit var _type: DataUpdateType
    private lateinit var _info: DataLatestInfo

    val info: DataLatestInfo
        get() = _info

    val type: DataUpdateType
        get() = _type

    fun setTargetData(type: DataUpdateType, info: DataLatestInfo) {
        _type = type
        _info = info
    }

    fun onResult(confirmed: Boolean) = viewModelScope.launch {
        appStateRepository.emitMessage(
            if(confirmed) {
                AppMessage.RequestDataUpdate(
                    info = info,
                    type = type,
                    confirmed = true,
                )
            } else {
                AppMessage.DataUpdateResult(
                    type = type,
                    success = false,
                )
            }
        )
    }
}

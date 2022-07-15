package jp.seo.station.ekisagasu.ui.dialog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.repository.AppStateRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfirmDataUpdateViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args by lazy {
        ConfirmDataUpdateDialogArgs.fromSavedStateHandle(savedStateHandle)
    }

    val info by lazy { args.info }

    val type by lazy { args.type }

    fun onResult(confirmed: Boolean) = viewModelScope.launch {
        appStateRepository.emitMessage(
            if (confirmed) {
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

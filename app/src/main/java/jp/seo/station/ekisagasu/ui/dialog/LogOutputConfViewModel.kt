package jp.seo.station.ekisagasu.ui.dialog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.repository.AppStateRepository
import jp.seo.station.ekisagasu.ui.log.LogOutputConfig
import jp.seo.station.ekisagasu.ui.log.LogOutputExtension
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogOutputConfViewModel
    @Inject
    constructor(
        private val appStateRepository: AppStateRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val config: LogOutputConfig by lazy {
            LogOutputConfDialogArgs.fromSavedStateHandle(savedStateHandle).config
        }

        private val _checked = MutableStateFlow(LogOutputExtension.txt)
        val checked =
            _checked
                .map {
                    when (it) {
                        LogOutputExtension.txt -> R.id.radio_button_txt
                        LogOutputExtension.gpx -> R.id.radio_button_gpx
                    }
                }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(),
                    R.id.radio_button_txt,
                )

        fun onChecked(id: Int) {
            _checked.update {
                when (id) {
                    R.id.radio_button_txt -> LogOutputExtension.txt
                    R.id.radio_button_gpx -> LogOutputExtension.gpx
                    else -> LogOutputExtension.txt
                }
            }
        }

        fun writeLog() =
            viewModelScope.launch {
                appStateRepository.emitMessage(
                    AppMessage.LogOutputConfigResolved(
                        LogOutputConfig.Geo(_checked.value),
                    ),
                )
            }
    }

package jp.seo.station.ekisagasu.ui.dialog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.ui.log.LogOutputConfig
import jp.seo.station.ekisagasu.ui.log.LogOutputExtension
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class LogOutputConfViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val config: LogOutputConfig by lazy {
        LogOutputConfDialogArgs.fromSavedStateHandle(savedStateHandle).config
    }

    private val _checked = MutableStateFlow(config.extension)
    val checked = _checked
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

    val currentConfig: LogOutputConfig
        get() = LogOutputConfig.Geo(_checked.value)
}

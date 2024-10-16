package com.seo4d696b75.android.ekisagasu.ui.log.output

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.log.LogOutputConfig
import com.seo4d696b75.android.ekisagasu.ui.log.LogOutputExtension
import dagger.hilt.android.lifecycle.HiltViewModel
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
        LogOutputConfDialogArgs.fromSavedStateHandle(savedStateHandle).config.value
    }

    private val _checked = MutableStateFlow(config.extension)
    val checked = _checked
        .map {
            when (it) {
                LogOutputExtension.TXT -> R.id.radio_button_txt
                LogOutputExtension.GPX -> R.id.radio_button_gpx
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
                R.id.radio_button_txt -> LogOutputExtension.TXT
                R.id.radio_button_gpx -> LogOutputExtension.GPX
                else -> LogOutputExtension.TXT
            }
        }
    }

    val currentConfig: LogOutputConfig
        get() = LogOutputConfig.Geo(_checked.value)
}

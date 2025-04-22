package com.seo4d696b75.android.ekisagasu.ui.log.output

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.seo4d696b75.android.ekisagasu.domain.error.ErrorHandler
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEventHolder
import com.seo4d696b75.android.ekisagasu.ui.event.navigationEventHolder
import com.seo4d696b75.android.ekisagasu.ui.log.LogOutputConfig
import com.seo4d696b75.android.ekisagasu.ui.log.LogOutputExtension
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationRoute
import com.seo4d696b75.android.ekisagasu.ui.navigation.typeMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class LogOutputConfigViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    errorHandler: ErrorHandler,
) : ViewModel(),
    ErrorHandler by errorHandler,
    NavigationEventHolder<LogOutputConfigViewModel.Nav> by navigationEventHolder() {

    private val args: NavigationRoute.Log.LogOutputConfigDialog = savedStateHandle.toRoute(typeMap)

    private val checked = MutableStateFlow(args.config.extension)
    val uiState = checked.asStateFlow()

    fun onExtensionChanged(extension: LogOutputExtension) {
        checked.update { extension }
    }

    fun onCancel() {
        navigate(Nav.Cancel)
    }

    fun onWriteClicked() = viewModelScope.launchCatching {
        val newConfig = args.config.copy(extension = checked.value)
        navigate(Nav.WriteLog(newConfig))
    }

    sealed interface Nav : NavigationEvent {
        data object Cancel : Nav
        data class WriteLog(val config: LogOutputConfig.Geo) : Nav
    }
}

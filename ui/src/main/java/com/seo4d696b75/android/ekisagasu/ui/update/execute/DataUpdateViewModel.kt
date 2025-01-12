package com.seo4d696b75.android.ekisagasu.ui.update.execute

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.seo4d696b75.android.ekisagasu.domain.dataset.LatestDataVersion
import com.seo4d696b75.android.ekisagasu.domain.dataset.update.DataUpdateType
import com.seo4d696b75.android.ekisagasu.domain.dataset.update.DataUpdateUseCase
import com.seo4d696b75.android.ekisagasu.domain.log.LogCollector
import com.seo4d696b75.android.ekisagasu.domain.log.LogMessage
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEventHolder
import com.seo4d696b75.android.ekisagasu.ui.event.navigationEventHolder
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationRoute
import com.seo4d696b75.android.ekisagasu.ui.navigation.typeMap
import com.seo4d696b75.android.ekisagasu.ui.update.NavigateDataUpdateEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DataUpdateViewModel @Inject constructor(
    private val updateData: DataUpdateUseCase,
    private val navigateDataUpdateEvent: NavigateDataUpdateEvent,
    savedStateHandle: SavedStateHandle,
    collector: LogCollector,
    @ApplicationContext private val context: Context,
) : ViewModel(),
    LogCollector by collector,
    NavigationEventHolder<DataUpdateViewModel.Nav> by navigationEventHolder() {

    private val args = savedStateHandle.toRoute<NavigationRoute.DataUpdate.ExecuteDialog>(typeMap)

    val uiState = updateData.progress

    private var updateJob: Job? = null

    fun update() {
        updateJob = viewModelScope.launch {
            updateData(
                info = args.info,
                dir = File(context.filesDir, "tmp"),
            ).onSuccess {
                log(LogMessage.Data.UpdateSuccess)
                navigate(Nav.Success)
            }.onFailure {
                // TODO non-fatalエラーログ送信する
                Timber.w(it)
                log(LogMessage.Data.UpdateFailure(it))
                navigate(Nav.Failure(args.type, args.info))
            }
        }
    }

    fun cancel() {
        updateJob?.cancel()
        navigate(Nav.Cancel)
        if (args.type == DataUpdateType.Init) {
            // TODO アプリ終了
        }
    }

    sealed interface Nav : NavigationEvent {
        data object Success : Nav
        data class Failure(val type: DataUpdateType, val info: LatestDataVersion) : Nav
        data object Cancel : Nav
    }
}

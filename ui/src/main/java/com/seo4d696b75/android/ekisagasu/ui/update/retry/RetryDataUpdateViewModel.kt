package com.seo4d696b75.android.ekisagasu.ui.update.retry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.seo4d696b75.android.ekisagasu.domain.dataset.LatestDataVersion
import com.seo4d696b75.android.ekisagasu.domain.dataset.update.DataUpdateType
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEventHolder
import com.seo4d696b75.android.ekisagasu.ui.event.navigationEventHolder
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationRoute
import com.seo4d696b75.android.ekisagasu.ui.navigation.typeMap
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RetryDataUpdateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel(),
    NavigationEventHolder<RetryDataUpdateViewModel.Nav> by navigationEventHolder() {

    private val args = savedStateHandle.toRoute<NavigationRoute.DataUpdate.RetryDialog>(typeMap)

    fun retry() {
        val nav = Nav.Retry(args.type, args.info)
        navigate(nav)
    }

    fun cancel() {
        navigate(Nav.Cancel)
        if (args.type == DataUpdateType.Init) {
            // TODO アプリ終了
        }
    }

    sealed interface Nav : NavigationEvent {
        data class Retry(val type: DataUpdateType, val info: LatestDataVersion) : Nav
        data object Cancel : Nav
    }
}

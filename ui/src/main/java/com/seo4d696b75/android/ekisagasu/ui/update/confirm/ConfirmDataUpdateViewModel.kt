package com.seo4d696b75.android.ekisagasu.ui.update.confirm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.seo4d696b75.android.ekisagasu.domain.dataset.LatestDataVersion
import com.seo4d696b75.android.ekisagasu.domain.dataset.update.DataUpdateType
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEventHolder
import com.seo4d696b75.android.ekisagasu.ui.event.navigationEventHolder
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationRoute
import com.seo4d696b75.android.ekisagasu.ui.navigation.typeMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfirmDataUpdateViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel(),
    NavigationEventHolder<ConfirmDataUpdateViewModel.Nav> by navigationEventHolder() {

    val args = savedStateHandle.toRoute<NavigationRoute.DataUpdate.ConfirmDialog>(typeMap)

    fun onResult(confirmed: Boolean) {
        if (confirmed) {
            val nav = Nav.ExecuteUpdate(args.type, args.info)
            navigate(nav)
        } else {
            navigate(Nav.CancelUpdate)
            if (args.type == DataUpdateType.Init) {
                viewModelScope.launch {
                    // データ不在のためアプリ継続不可
                    appStateRepository.requestAppFinish()
                }
            }
        }
    }

    sealed interface Nav : NavigationEvent {
        data object CancelUpdate : Nav
        data class ExecuteUpdate(val type: DataUpdateType, val info: LatestDataVersion) : Nav
    }
}

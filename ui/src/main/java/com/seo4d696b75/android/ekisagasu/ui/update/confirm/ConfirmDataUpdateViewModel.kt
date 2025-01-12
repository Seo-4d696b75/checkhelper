package com.seo4d696b75.android.ekisagasu.ui.update.confirm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.seo4d696b75.android.ekisagasu.domain.dataset.update.DataUpdateType
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationRoute
import com.seo4d696b75.android.ekisagasu.ui.navigation.typeMap
import com.seo4d696b75.android.ekisagasu.ui.update.DataUpdateNavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.update.NavigateDataUpdateEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfirmDataUpdateViewModel @Inject constructor(
    private val navigateDataUpdateEvent: NavigateDataUpdateEvent,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val args = savedStateHandle.toRoute<NavigationRoute.DataUpdate.ConfirmUpdateDialog>(typeMap)

    fun onResult(confirmed: Boolean) = viewModelScope.launch {
        if (confirmed) {
            navigateDataUpdateEvent(DataUpdateNavigationEvent.RequestUpdate(args.type, args.info))
        } else if (args.type == DataUpdateType.Init) {
            // データ不在のためアプリ継続不可
            // TODO アプリ終了
        }
    }
}

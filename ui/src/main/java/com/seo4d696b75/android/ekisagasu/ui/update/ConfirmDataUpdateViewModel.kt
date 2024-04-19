package com.seo4d696b75.android.ekisagasu.ui.update

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.message.AppMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    fun onResult(confirmed: Boolean) =
        viewModelScope.launch {
            appStateRepository.emitMessage(
                if (confirmed) {
                    AppMessage.Data.RequestUpdate(
                        info = info,
                        type = type,
                    )
                } else {
                    AppMessage.Data.CancelUpdate(
                        type = type,
                    )
                },
            )
        }
}

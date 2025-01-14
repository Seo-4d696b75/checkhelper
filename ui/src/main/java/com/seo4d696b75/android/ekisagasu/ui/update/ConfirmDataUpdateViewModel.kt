package com.seo4d696b75.android.ekisagasu.ui.update

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ConfirmDataUpdateViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args by lazy {
        ConfirmDataUpdateDialogArgs.fromSavedStateHandle(savedStateHandle)
    }

    val info by lazy { args.info.value }

    val type by lazy { args.type }

    fun onResult(confirmed: Boolean) {}
}

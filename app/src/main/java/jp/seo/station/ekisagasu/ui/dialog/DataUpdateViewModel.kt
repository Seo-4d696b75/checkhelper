package jp.seo.station.ekisagasu.ui.dialog

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.seo.station.ekisagasu.database.DataVersion
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.repository.AppStateRepository
import jp.seo.station.ekisagasu.usecase.DataUpdateUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DataUpdateViewModel @Inject constructor(
    private val updateData: DataUpdateUseCase,
    private val appStateRepository: AppStateRepository,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val args by lazy {
        ConfirmDataUpdateDialogArgs.fromSavedStateHandle(savedStateHandle)
    }

    val info by lazy { args.info }

    val type by lazy { args.type }

    val progress = updateData.progress

    private val _result = MutableSharedFlow<Result<DataVersion>>()

    val result: SharedFlow<Result<DataVersion>> = _result

    fun update() = viewModelScope.launch {
        val result = updateData(info, File(context.filesDir, "tmp"))
        _result.emit(result)
    }

    fun onResult(success: Boolean) = viewModelScope.launch {
        appStateRepository.emitMessage(
            AppMessage.DataUpdateResult(
                type = type,
                success = success,
            )
        )
    }
}

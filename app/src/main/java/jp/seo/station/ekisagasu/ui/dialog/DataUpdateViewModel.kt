package jp.seo.station.ekisagasu.ui.dialog

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.message.AppMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataVersion
import com.seo4d696b75.android.ekisagasu.domain.dataset.update.DataUpdateUseCase
import com.seo4d696b75.android.ekisagasu.domain.log.LogCollector
import com.seo4d696b75.android.ekisagasu.domain.log.LogMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DataUpdateViewModel @Inject constructor(
    private val updateData: DataUpdateUseCase,
    private val appStateRepository: AppStateRepository,
    private val savedStateHandle: SavedStateHandle,
    private val collector: LogCollector,
    @ApplicationContext private val context: Context,
) : ViewModel(),
    LogCollector by collector {
    private val args by lazy {
        ConfirmDataUpdateDialogArgs.fromSavedStateHandle(savedStateHandle)
    }

    val info by lazy { args.info }

    val type by lazy { args.type }

    val progress = updateData.progress

    private val _result = MutableSharedFlow<Result<DataVersion>>()

    val result = _result.asSharedFlow()

    private var job: Job? = null

    fun update() {
        assert(job == null)
        job = viewModelScope.launch {
            val result = updateData(
                info = info,
                dir = File(context.filesDir, "tmp"),
            ).onSuccess {
                appStateRepository.emitMessage(
                    AppMessage.Data.UpdateSuccess
                )
                log(LogMessage.Data.UpdateSuccess)
            }.onFailure {
                Timber.w(it)
                appStateRepository.emitMessage(
                    AppMessage.Data.UpdateFailure(type, it)
                )
                log(LogMessage.Data.UpdateFailure(it))
            }
            _result.emit(result)
        }
    }

    fun onCancel() {
        job?.cancel()
    }
}

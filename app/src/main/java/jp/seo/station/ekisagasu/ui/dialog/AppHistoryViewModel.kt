package jp.seo.station.ekisagasu.ui.dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.data.database.AppRebootLog
import com.seo4d696b75.android.ekisagasu.data.log.LogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppHistoryViewModel
    @Inject
    constructor(
        private val logRepository: LogRepository,
    ) : ViewModel() {
        val history = logRepository.history

        fun setLogTarget(target: AppRebootLog) =
            viewModelScope.launch {
                logRepository.filterLogSince(target)
            }
    }

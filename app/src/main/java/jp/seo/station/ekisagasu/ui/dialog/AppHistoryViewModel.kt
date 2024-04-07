package jp.seo.station.ekisagasu.ui.dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.database.AppRebootLog
import jp.seo.station.ekisagasu.repository.LogRepository
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

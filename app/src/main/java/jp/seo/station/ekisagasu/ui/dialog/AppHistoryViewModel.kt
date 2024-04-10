package jp.seo.station.ekisagasu.ui.dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogTarget
import com.seo4d696b75.android.ekisagasu.domain.log.LogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppHistoryViewModel @Inject constructor(
    private val logRepository: LogRepository,
) : ViewModel() {
    val history = logRepository.history

    fun setLogTarget(target: AppLogTarget) = viewModelScope.launch {
        logRepository.setTarget(target)
    }
}

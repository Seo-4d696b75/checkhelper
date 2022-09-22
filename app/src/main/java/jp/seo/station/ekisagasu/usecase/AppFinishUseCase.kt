package jp.seo.station.ekisagasu.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.seo.station.ekisagasu.repository.AppStateRepository
import jp.seo.station.ekisagasu.repository.LogRepository
import javax.inject.Inject

class AppFinishUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logRepository: LogRepository,
    private val appStateRepository: AppStateRepository,
) {
    suspend operator fun invoke() {
        appStateRepository.isServiceRunning = false
        logRepository.onAppFinish(context)

        // reset
        appStateRepository.setTimerFixed(false)
        appStateRepository.setNightMode(false)
    }
}

package jp.seo.station.ekisagasu.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.seo.station.ekisagasu.repository.AppStateRepository
import jp.seo.station.ekisagasu.repository.LogRepository
import jp.seo.station.ekisagasu.repository.UserSettingRepository
import javax.inject.Inject

class AppFinishUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logRepository: LogRepository,
    private val appStateRepository: AppStateRepository,
    private val userSettingRepository: UserSettingRepository
) {
    suspend operator fun invoke() {
        appStateRepository.isServiceRunning = false
        logRepository.onAppFinish(context)
        userSettingRepository.save()

        // reset
        appStateRepository.setTimerFixed(false)
        appStateRepository.setNightMode(false)
    }
}

package jp.seo.station.ekisagasu.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.seo.station.ekisagasu.repository.AppStateRepository
import jp.seo.station.ekisagasu.repository.LogRepository
import jp.seo.station.ekisagasu.repository.UserSettingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@ExperimentalCoroutinesApi
class AppFinishUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userSettingRepository: UserSettingRepository,
    private val logRepository: LogRepository,
    private val appStateRepository: AppStateRepository,
){
    suspend operator fun invoke() {
        appStateRepository.isServiceRunning = false
        userSettingRepository.save()
        logRepository.onAppFinish(context)

        // reset
        appStateRepository.setTimerFixed(false)
        appStateRepository.setNightMode(false)
    }
}
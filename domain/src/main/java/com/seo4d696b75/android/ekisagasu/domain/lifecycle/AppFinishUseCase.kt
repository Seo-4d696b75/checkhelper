package com.seo4d696b75.android.ekisagasu.domain.lifecycle

import com.seo4d696b75.android.ekisagasu.domain.log.LogRepository
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.domain.user.UserSettingRepository
import javax.inject.Inject

class AppFinishUseCase @Inject constructor(
    private val logRepository: LogRepository,
    private val appStateRepository: AppStateRepository,
    private val userSettingRepository: UserSettingRepository,
) {
    suspend operator fun invoke() {
        appStateRepository.isServiceRunning = false
        logRepository.onAppFinish()
        userSettingRepository.save()

        // reset
        appStateRepository.setTimerFixed(false)
        appStateRepository.setNightMode(false)
    }
}

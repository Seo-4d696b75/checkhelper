package com.seo4d696b75.android.ekisagasu.domain.lifecycle

import com.seo4d696b75.android.ekisagasu.domain.dataset.PrefectureRepository
import com.seo4d696b75.android.ekisagasu.domain.log.LogRepository
import com.seo4d696b75.android.ekisagasu.domain.user.UserSettingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BootUseCase @Inject constructor(
    private val logRepository: LogRepository,
    private val prefectureRepository: PrefectureRepository,
    private val userSettingRepository: UserSettingRepository,
) {
    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        logRepository.onAppBoot()
        prefectureRepository.setData()
        userSettingRepository.load()
    }
}

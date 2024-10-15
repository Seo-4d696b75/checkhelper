package com.seo4d696b75.android.ekisagasu.domain.lifecycle

import com.seo4d696b75.android.ekisagasu.domain.dataset.PrefectureRepository
import com.seo4d696b75.android.ekisagasu.domain.log.LogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BootUseCase @Inject constructor(
    private val logRepository: LogRepository,
    private val prefectureRepository: PrefectureRepository,
) {
    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        logRepository.onAppBoot()
        prefectureRepository.setData()
    }
}

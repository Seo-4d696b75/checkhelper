package jp.seo.station.ekisagasu.usecase

import android.content.Context
import com.seo4d696b75.android.ekisagasu.data.log.LogRepository
import com.seo4d696b75.android.ekisagasu.data.station.PrefectureRepository
import com.seo4d696b75.android.ekisagasu.data.user.UserSettingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BootUseCase
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val logRepository: LogRepository,
        private val prefectureRepository: PrefectureRepository,
        private val userSettingRepository: UserSettingRepository,
    ) {
        suspend operator fun invoke() =
            withContext(Dispatchers.IO) {
                logRepository.onAppBoot(context)
                prefectureRepository.setData(context)
                userSettingRepository.load()
            }
    }

package jp.seo.station.ekisagasu.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.seo.station.ekisagasu.repository.PrefectureRepository
import jp.seo.station.ekisagasu.repository.LogRepository
import jp.seo.station.ekisagasu.repository.UserSettingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BootUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logRepository: LogRepository,
    private val settingRepository: UserSettingRepository,
    private val prefectureRepository: PrefectureRepository,
) {

    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        logRepository.onAppBoot(context)
        settingRepository.load()
        prefectureRepository.setData(context)
    }
}
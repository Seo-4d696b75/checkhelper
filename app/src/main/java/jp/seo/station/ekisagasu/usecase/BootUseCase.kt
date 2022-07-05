package jp.seo.station.ekisagasu.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.seo.station.ekisagasu.core.PrefectureRepository
import jp.seo.station.ekisagasu.core.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BootUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val prefectureRepository: PrefectureRepository,
) {

    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        userRepository.onAppReboot(context)
        prefectureRepository.setData(context)
    }
}
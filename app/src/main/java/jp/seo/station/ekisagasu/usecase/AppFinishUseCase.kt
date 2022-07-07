package jp.seo.station.ekisagasu.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.seo.station.ekisagasu.core.UserRepository
import jp.seo.station.ekisagasu.repository.AppStateRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@ExperimentalCoroutinesApi
class AppFinishUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val appStateRepository: AppStateRepository,
){
    suspend operator fun invoke() {
        appStateRepository.isServiceRunning = false
        userRepository.onAppFinish(context)

        // reset
        appStateRepository.setTimerFixed(false)
        appStateRepository.setNightMode(false)
    }
}
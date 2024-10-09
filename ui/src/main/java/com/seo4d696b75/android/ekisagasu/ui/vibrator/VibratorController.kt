package com.seo4d696b75.android.ekisagasu.ui.vibrator

import android.app.Service
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorPrediction
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorRepository
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorState
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.domain.user.UserSettingRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class VibratorController @Inject constructor(
    private val searchRepository: StationSearchRepository,
    private val settingRepository: UserSettingRepository,
    private val navigatorRepository: NavigatorRepository,
) {
    companion object {
        private val VIBRATE_PATTERN_NORMAL = longArrayOf(0, 500, 100, 100)
        private val VIBRATE_PATTERN_ALERT = longArrayOf(0, 500, 100, 100, 100, 100, 100, 100)
        private val VIBRATE_PATTERN_APPROACH = longArrayOf(0, 100, 100, 100, 100, 100)
    }

    private var vibrator: Vibrator? = null
    private var isVibrate: Boolean = false
    private var isVibrateWhenApproach: Boolean = false

    private var currentLine: Line? = null
    private var vibrateMeterWhenApproach: Int = 100
    private var hasApproach: Boolean = false
    private var nextApproachStation: Station? = null

    fun onCreate(context: Context, owner: LifecycleOwner) {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(Service.VIBRATOR_MANAGER_SERVICE)?.let {
                val manager = it as VibratorManager
                manager.defaultVibrator
            }
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Service.VIBRATOR_SERVICE) as Vibrator
        }

        owner.lifecycleScope.launch {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    // 近傍駅の変更（距離の変化は無視）
                    searchRepository
                        .result
                        .filterNotNull()
                        .map { it.detected.station }
                        .distinctUntilChanged()
                        .collect(::onStationChanged)
                }
                launch {
                    // 選択路線の変化
                    searchRepository.selectedLine.collect {
                        currentLine = it
                    }
                }
                launch {
                    // 設定値の変化
                    settingRepository.setting.collect {
                        isVibrate = it.isVibrate
                        isVibrateWhenApproach = it.isVibrateWhenApproach
                        vibrateMeterWhenApproach = it.vibrateDistance
                    }
                }
                launch {
                    // 路線navigatorの予測変化
                    navigatorRepository
                        .state
                        .filterIsInstance<NavigatorState.Result>()
                        .map { it.predictions.firstOrNull() }
                        .filterNotNull()
                        .collect(::onStationPredictionChanged)
                }
            }
        }
    }

    private fun onStationChanged(s: Station) {
        val line = currentLine
        if (line != null && !s.isLine(line)) {
            vibrate(VIBRATE_PATTERN_ALERT)
        } else {
            vibrate(VIBRATE_PATTERN_NORMAL)
        }
    }

    private fun onStationPredictionChanged(next: NavigatorPrediction) {
        if (nextApproachStation == null || nextApproachStation != next.station) {
            nextApproachStation = next.station
            hasApproach = false
        } else if (
            isVibrateWhenApproach && !hasApproach && next.distance < vibrateMeterWhenApproach
        ) {
            hasApproach = true
            vibrate(VIBRATE_PATTERN_APPROACH)
        }
    }

    private fun vibrate(pattern: LongArray) {
        if (!isVibrate || vibrator?.hasVibrator() != true) return
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    fun onDestroy() {
        vibrator = null
    }
}

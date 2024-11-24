package com.seo4d696b75.android.ekisagasu.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.location.LocationState
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorRepository
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorState
import com.seo4d696b75.android.ekisagasu.domain.screen.PopupStatus
import com.seo4d696b75.android.ekisagasu.domain.screen.PopupStatusRepository
import com.seo4d696b75.android.ekisagasu.domain.screen.ScreenRepository
import com.seo4d696b75.android.ekisagasu.domain.screen.ScreenStatus
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.domain.user.UserSettingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@SuppressLint("ClickableViewAccessibility")
@Singleton
class OverlayViewController @Inject constructor(
    private val searchRepository: StationSearchRepository,
    private val settingRepository: UserSettingRepository,
    private val appStateRepository: AppStateRepository,
    private val screenRepository: ScreenRepository,
    locationRepository: LocationRepository,
    navigatorRepository: NavigatorRepository,
) : PopupStatusRepository {
    private lateinit var windowManager: WindowManager

    private lateinit var keepOnScreen: View
    private lateinit var touchScreen: View
    private lateinit var darkScreen: View

    private var _lifecycleScope: CoroutineScope? = null
    private val scope: CoroutineScope
        get() = requireNotNull(_lifecycleScope)

    fun onCreate(context: Context, owner: LifecycleOwner) {
        _lifecycleScope = owner.lifecycleScope
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layerType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        // transparent & not touchable view so that screen kept turn on
        val keepOnLayoutParam = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            0,
            0,
            layerType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        )
        keepOnScreen = View(context)
        keepOnScreen.visibility = View.GONE
        windowManager.addView(keepOnScreen, keepOnLayoutParam)

        val darkLayoutParam = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            0,
            0,
            layerType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        )
        darkScreen = View(context)
        darkScreen.visibility = View.GONE
        windowManager.addView(darkScreen, darkLayoutParam)

        // zero size view to watch any touch event and not consume any touch event
        val touchLayoutParam = WindowManager.LayoutParams(
            0,
            0,
            0,
            0,
            layerType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        )
        touchScreen = View(context).apply {
            setOnTouchListener { _, _ ->
                if (keepOnScreen.visibility == View.VISIBLE) {
                    closePopup()
                } else {
                    invalidateNightScrim(nightMode, nightModeTimeout)
                }
                false
            }
            isClickable = false
            isLongClickable = false
        }
        windowManager.addView(touchScreen, touchLayoutParam)


        owner.lifecycleScope.launch {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    // 設定の変更
                    settingRepository.setting.collect {
                        notify = it.isPushNotification
                        forceNotify = it.isPushNotificationForce
                        keepNotification = it.isKeepNotification
                        nightModeTimeout = it.nightModeTimeout
                        brightness = it.nightModeBrightness
                    }
                }
                launch {
                    // nightモード切替
                    appStateRepository.nightMode.collect {
                        nightMode = it
                        invalidateNightScrim(it, nightModeTimeout)
                    }
                }
                launch {
                    // 点灯状態の監視
                    screenRepository.status.collect {
                        screenStatus = it
                        if (it.isContentVisible && isPopupPending) {
                            // 暗転中に駅変化があった場合は通知
                            isPopupPending = false
                            showPopup(true)
                        }
                        setNightScrimTimer()
                    }
                }
                launch {
                    // 現在の最近傍駅の変化（距離の変化は無視する）
                    searchRepository
                        .result
                        .filterNotNull()
                        .map { it.detected }
                        .distinctUntilChanged()
                        .collect {
                            if (screenStatus.isContentVisible) {
                                // 一定時間経過で消す
                                showPopup(true)
                            } else if (screenStatus is ScreenStatus.TurnOff && forceNotify) {
                                // 画面を強制的に点灯する
                                val intent = Intent(context, WakeupActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                                if (screenRepository.isScreenLocked) {
                                    // 画面が点灯＆スクリーンロック解除まで待つ
                                    isPopupPending = true
                                } else {
                                    // 即座に画面点灯＆ポップアップを表示する
                                    showPopup(false)
                                    keepOnScreen.visibility = View.VISIBLE
                                    darkScreen.visibility = View.VISIBLE
                                }
                            } else {
                                // 画面が点灯＆スクリーンロック解除まで待つ
                                isPopupPending = true
                            }
                        }
                }
            }
        }
    }

    private var screenStatus: ScreenStatus = ScreenStatus.TurnOn(false)
    private var notify = false
    private var forceNotify = false
    private var keepNotification = false
    private var nightMode = false

    // スクリムを再表示するまでの時間
    private var nightModeTimeout: Int = 0
        set(value) {
            if (value == field || value < 0) return
            field = value
            invalidateNightScrim(nightMode, value)
        }

    // スクリムの明るさ
    private var brightness: Float = 255f
        set(value) {
            if (value != field && value >= MIN_BRIGHTNESS && value < 256f) {
                field = value
                val black = ColorDrawable((255 - value.roundToInt()).shl(24))
                darkScreen.background = black
            }
        }

    /**
     * 夜間モードのスクリムViewの状態を更新する
     */
    private fun invalidateNightScrim(
        enable: Boolean,
        timeout: Int,
    ) {
        if (enable) {
            if (timeout == 0) {
                darkScreen.visibility = View.VISIBLE
            } else {
                darkScreen.visibility = View.GONE
                setNightScrimTimer()
            }
        } else {
            nightModeTimeoutJob?.cancel()
            darkScreen.visibility = View.GONE
        }
    }

    private var nightModeTimeoutJob: Job? = null

    /**
     * 一定時間が経過したらスクリムを再表示する
     */
    private fun setNightScrimTimer() {
        if (nightModeTimeout > 0 && nightMode) {
            nightModeTimeoutJob?.cancel()
            nightModeTimeoutJob = scope.launch {
                delay(1000L * nightModeTimeout)
                darkScreen.visibility = View.VISIBLE
                nightModeTimeoutJob = null
            }
        }
    }

    private var isPopupPending = false
    private val isPopupVisible = MutableStateFlow(false)
    private var popupTimerJob: Job? = null

    /**
     * ポップアップを表示する
     *
     * @param timer `true`の場合は一定時間経過に非表示にする
     */
    private fun showPopup(timer: Boolean) {
        if (!notify || keepNotification) {
            return
        }
        isPopupVisible.update { true }
        popupTimerJob?.cancel()
        if (timer) {
            popupTimerJob = scope.launch {
                delay(5000L)
                isPopupVisible.update { false }
            }
        }
    }

    override fun closePopup() {
        if (notify && !keepNotification) {
            popupTimerJob?.cancel()
            isPopupVisible.update { false }
            keepOnScreen.visibility = View.GONE
            invalidateNightScrim(nightMode, nightModeTimeout)
        }
    }

    private val isExpanded = MutableStateFlow(true)

    override fun togglePopup() {
        if (notify && keepNotification) {
            isExpanded.update { !it }
            keepOnScreen.visibility = View.GONE
            invalidateNightScrim(nightMode, nightModeTimeout)
        }
    }

    // ポップの表示状態
    @OptIn(ExperimentalCoroutinesApi::class)
    override val status: Flow<PopupStatus> = combine(
        settingRepository.setting,
        locationRepository.currentLocation,
        navigatorRepository.state,
    ) { setting, location, navigation ->
        val enabled = setting.isPushNotification &&
            location is LocationState.Result &&
            navigation !is NavigatorState.Running
        val keep = setting.isKeepNotification
        enabled to keep
    }
        .distinctUntilChanged()
        .transformLatest { pair ->
            val (enabled, keep) = pair
            if (!enabled) {
                // 常に非表示
                emit(PopupStatus.Invisible)
            } else if (keep) {
                // 常に表示
                isExpanded.update { true }
                emitAll(
                    isExpanded.map {
                        PopupStatus.Visible.Fixed(isExpanded = it)
                    }
                )
            } else {
                isPopupVisible.update { false }
                emitAll(
                    isPopupVisible.map {
                        if (it) {
                            PopupStatus.Visible.Closable
                        } else {
                            PopupStatus.Invisible
                        }
                    }
                )
            }
        }
        .distinctUntilChanged()

    fun onDestroy() {
        windowManager.removeView(keepOnScreen)
        windowManager.removeViewImmediate(touchScreen)
        windowManager.removeView(darkScreen)
        nightMode = false

        touchScreen.setOnTouchListener(null)

        nightModeTimeoutJob?.cancel()
        nightModeTimeoutJob = null

        _lifecycleScope = null
    }

    companion object {
        const val MIN_BRIGHTNESS = 20f
    }
}

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface PopupStatusRepositoryModule {
    @Binds
    fun binds(impl: OverlayViewController): PopupStatusRepository
}

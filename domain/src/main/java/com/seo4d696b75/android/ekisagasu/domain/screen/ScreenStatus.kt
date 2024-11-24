package com.seo4d696b75.android.ekisagasu.domain.screen

sealed interface ScreenStatus {
    data object TurnOff : ScreenStatus
    data class TurnOn(val isLocked: Boolean) : ScreenStatus

    /** 画面が点灯しているかつロックスクリーン（keyguard）が解除されている */
    val isContentVisible: Boolean
        get() = when (this) {
            TurnOff -> false
            is TurnOn -> !isLocked
        }
}

package com.seo4d696b75.android.ekisagasu.domain.screen

sealed interface PopupStatus {
    /** 非表示 */
    data object Invisible : PopupStatus

    /** 表示 */
    data class Visible(
        /** ユーザー操作やタイマーによって非表示にしない */
        val shouldKeep: Boolean,
    ) : PopupStatus
}

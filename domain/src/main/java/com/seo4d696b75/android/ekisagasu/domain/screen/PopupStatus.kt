package com.seo4d696b75.android.ekisagasu.domain.screen

sealed interface PopupStatus {
    /** 非表示 */
    data object Invisible : PopupStatus

    /** 表示 */
    sealed interface Visible : PopupStatus {
        val isExpanded: Boolean

        /** 表示し続ける ただし縮小可能 */
        data class Fixed(override val isExpanded: Boolean) : Visible

        /** ユーザー操作により非表示可能 */
        data object Closable : Visible {
            override val isExpanded = true
        }
    }
}

package com.seo4d696b75.android.ekisagasu.ui.line

import androidx.annotation.StringRes
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class LineSelectUiState(
    @StringRes
    val message: Int?,
    val clearEnabled: Boolean,
    val lines: ImmutableList<Line>,
) {
    companion object {
        val Initial = LineSelectUiState(
            message = null,
            clearEnabled = false,
            lines = persistentListOf(),
        )
    }
}

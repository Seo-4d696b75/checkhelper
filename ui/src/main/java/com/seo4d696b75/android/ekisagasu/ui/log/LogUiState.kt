package com.seo4d696b75.android.ekisagasu.ui.log

import androidx.compose.runtime.Immutable
import com.seo4d696b75.android.ekisagasu.domain.log.AppLog
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogTarget
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogType
import kotlinx.collections.immutable.ImmutableList

@Immutable
sealed interface LogUiState {
    data object Initializing : LogUiState

    data class Loaded(
        val filter: AppLogType.Filter,
        val target: AppLogTarget,
        val logs: ImmutableList<AppLog>,
    ) : LogUiState
}

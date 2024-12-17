package com.seo4d696b75.android.ekisagasu.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection

@Composable
operator fun PaddingValues.plus(other: PaddingValues): PaddingValues {
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        top = this.calculateTopPadding() + other.calculateTopPadding(),
        bottom = this.calculateBottomPadding() + other.calculateBottomPadding(),
        start = this.calculateStartPadding(direction) + other.calculateStartPadding(direction),
        end = this.calculateEndPadding(direction) + other.calculateEndPadding(direction),
    )
}

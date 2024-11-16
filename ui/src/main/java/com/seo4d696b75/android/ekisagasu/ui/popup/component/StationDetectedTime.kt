package com.seo4d696b75.android.ekisagasu.ui.popup.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.seo4d696b75.android.ekisagasu.ui.R

@Immutable
sealed interface StationDetectedTime {
    data object Now : StationDetectedTime
    data object Sec : StationDetectedTime
    data class Min(val min: Int) : StationDetectedTime

    @Composable
    fun text() = when (this) {
        Now -> stringResource(id = R.string.notification_time_now)
        Sec -> stringResource(id = R.string.notification_time_sec)
        is Min -> "$min${stringResource(id = R.string.notification_time_min)}"
    }
}

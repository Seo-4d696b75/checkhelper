package com.seo4d696b75.android.ekisagasu.ui.setting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataVersion
import com.seo4d696b75.android.ekisagasu.ui.R

@Immutable
sealed interface SettingUiState {
    data object Initializing : SettingUiState

    data class Loaded(
        val locationUpdateInterval: SettingItemUiState<Int>,
        val searchSize: SettingItemUiState<Int>,
        val isPopupEnabled: SettingItemUiState<Boolean>,
        val isPopupForced: SettingItemUiState<Boolean>,
        val isPopupKept: SettingItemUiState<Boolean>,
        val isPopupPrefectureShown: SettingItemUiState<Boolean>,
        val isVibrateEnabled: SettingItemUiState<Boolean>,
        val isVibrateOnApproachEnabled: SettingItemUiState<Boolean>,
        val vibrateDistanceOnApproach: SettingItemUiState<Int>,
        val nightScrimTimeout: SettingItemUiState<NightScrimTimeout>,
        val nightScrimBrightness: SettingItemUiState<Float>,
        val isNightMode: Boolean,
        val data: DataVersionUiState,
    ) : SettingUiState
}

@Immutable
data class SettingItemUiState<T>(
    val value: T,
    val enabled: Boolean,
    val onChange: (T) -> Unit,
) {
    companion object {
        fun <T> initial(initial: T) = SettingItemUiState(
            value = initial,
            enabled = false,
            onChange = {},
        )
    }
}

@Immutable
sealed interface DataVersionUiState {
    data object Undefined : DataVersionUiState

    @Immutable
    sealed interface Defined : DataVersionUiState {
        val version: DataVersion
    }

    data class Idle(override val version: DataVersion) : Defined
    data class Checking(override val version: DataVersion) : Defined
    data class LatestChecked(override val version: DataVersion) : Defined
}

@Immutable
sealed interface NightScrimTimeout {
    val seconds: Int

    @get:Composable
    val label: String

    data object Zero : NightScrimTimeout {
        override val seconds = 0
        override val label: String
            @Composable
            get() = stringResource(id = R.string.setting_mes_night_switch_always)
    }

    data class Positive(
        override val seconds: Int
    ) : NightScrimTimeout {
        override val label: String
            @Composable
            get() = stringResource(id = R.string.setting_mes_night_switch, seconds)
    }

    companion object {
        val Values = listOf(
            Zero,
            Positive(3),
            Positive(5),
            Positive(10),
            Positive(15),
            Positive(30),
            Positive(60),
            Positive(120),
            Positive(300),
        )
    }
}

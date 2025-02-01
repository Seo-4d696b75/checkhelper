package com.seo4d696b75.android.ekisagasu.ui.setting

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seo4d696b75.android.ekisagasu.ui.setting.section.SettingListSection
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme

@Composable
fun SettingScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingScreen(
        state = state,
        onNightModeChanged = viewModel::updateNightMode,
        checkLatestData = viewModel::checkLatestData,
        modifier = modifier,
    )
}

@SuppressLint("UnusedCrossfadeTargetStateParameter")
@Composable
fun SettingScreen(
    state: SettingUiState,
    onNightModeChanged: (Boolean) -> Unit,
    checkLatestData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        modifier = modifier.fillMaxSize(),
        targetState = state.javaClass,
        label = "SettingScreen SettingUiState",
    ) {
        when (state) {
            SettingUiState.Initializing -> {}

            is SettingUiState.Loaded ->
                SettingListSection(
                    state = state,
                    onNightModeChanged = onNightModeChanged,
                    checkLatestData = checkLatestData,
                )
        }
    }
}

@Composable
@PreviewLightDark
private fun SettingScreenPreview() {
    AppTheme {
        Surface {
            SettingScreen(
                onNightModeChanged = {},
                checkLatestData = {},
                state = SettingUiState.Loaded(
                    locationUpdateInterval = SettingItemUiState.initial(5),
                    searchSize = SettingItemUiState.initial(12),
                    isPopupEnabled = SettingItemUiState.initial(false),
                    isPopupForced = SettingItemUiState.initial(false),
                    isPopupKept = SettingItemUiState.initial(false),
                    isPopupPrefectureShown = SettingItemUiState.initial(false),
                    isVibrateEnabled = SettingItemUiState.initial(false),
                    isVibrateOnApproachEnabled = SettingItemUiState.initial(false),
                    vibrateDistanceOnApproach = SettingItemUiState.initial(100),
                    nightScrimTimeout = SettingItemUiState.initial(NightScrimTimeout.Zero),
                    nightScrimBrightness = SettingItemUiState.initial(128f),
                    isNightMode = false,
                    data = DataVersionUiState.Undefined,
                )
            )
        }
    }
}

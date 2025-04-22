package com.seo4d696b75.android.ekisagasu.ui.setting.section

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.common.plus
import com.seo4d696b75.android.ekisagasu.ui.overlay.OverlayViewController
import com.seo4d696b75.android.ekisagasu.ui.setting.NightScrimTimeout
import com.seo4d696b75.android.ekisagasu.ui.setting.SettingUiState
import com.seo4d696b75.android.ekisagasu.ui.setting.component.SettingGroup
import com.seo4d696b75.android.ekisagasu.ui.setting.component.SettingItem
import com.seo4d696b75.compose.material3.picker.NumberPicker
import kotlinx.collections.immutable.toPersistentList
import kotlin.math.roundToInt

@Composable
fun SettingListSection(
    state: SettingUiState.Loaded,
    onNightModeChanged: (Boolean) -> Unit,
    checkLatestData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp) +
            WindowInsets
                .safeDrawing
                .only(WindowInsetsSides.Vertical)
                .asPaddingValues(),
    ) {
        item {
            SettingGroup(
                icon = painterResource(id = R.drawable.ic_interval),
            ) {
                SettingItem(
                    title = stringResource(id = R.string.setting_title_freq),
                    description = stringResource(id = R.string.setting_mes_freq),
                ) {
                    NumberPicker(
                        value = state.locationUpdateInterval.value,
                        onValueChange = state.locationUpdateInterval.onChange,
                        range = (1..60).toPersistentList(),
                        enabled = state.locationUpdateInterval.enabled,
                        modifier = Modifier.width(80.dp),
                    )
                }
            }
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 16.dp,
                ),
            )
        }
        item {
            SettingGroup(
                icon = painterResource(id = R.drawable.ic_radar),
            ) {
                SettingItem(
                    title = stringResource(id = R.string.setting_title_radar),
                    description = stringResource(id = R.string.setting_mes_radar),
                ) {
                    NumberPicker(
                        value = state.searchSize.value,
                        onValueChange = state.searchSize.onChange,
                        range = (12..20).toPersistentList(),
                        enabled = state.searchSize.enabled,
                        modifier = Modifier.width(80.dp),
                    )
                }
            }
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 16.dp,
                ),
            )
        }
        item {
            SettingGroup(
                icon = painterResource(id = R.drawable.ic_popup),
            ) {
                SettingItem(
                    title = stringResource(id = R.string.setting_title_notification),
                    description = stringResource(id = R.string.setting_mes_notification),
                ) {
                    Switch(
                        checked = state.isPopupEnabled.value,
                        onCheckedChange = state.isPopupEnabled.onChange,
                        enabled = state.isPopupEnabled.enabled,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                SettingItem(
                    title = stringResource(id = R.string.setting_title_force_notify),
                    description = stringResource(id = R.string.setting_mes_force_notify),
                ) {
                    Switch(
                        checked = state.isPopupForced.value,
                        onCheckedChange = state.isPopupForced.onChange,
                        enabled = state.isPopupForced.enabled,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                SettingItem(
                    title = stringResource(id = R.string.setting_title_notify_stationary),
                    description = stringResource(id = R.string.setting_mes_notify_stationary),
                ) {
                    Switch(
                        checked = state.isPopupKept.value,
                        onCheckedChange = state.isPopupKept.onChange,
                        enabled = state.isPopupKept.enabled,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                SettingItem(
                    title = stringResource(id = R.string.setting_title_notify_prefecture),
                    description = stringResource(id = R.string.setting_mes_notify_prefecture),
                ) {
                    Switch(
                        checked = state.isPopupPrefectureShown.value,
                        onCheckedChange = state.isPopupPrefectureShown.onChange,
                        enabled = state.isPopupPrefectureShown.enabled,
                    )
                }
            }
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 16.dp,
                ),
            )
        }
        item {
            SettingGroup(
                icon = painterResource(id = R.drawable.ic_vibration),
            ) {
                SettingItem(
                    title = stringResource(id = R.string.setting_title_vibe),
                    description = stringResource(id = R.string.setting_mes_vibe),
                ) {
                    Switch(
                        checked = state.isVibrateEnabled.value,
                        onCheckedChange = state.isVibrateEnabled.onChange,
                        enabled = state.isVibrateEnabled.enabled,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                SettingItem(
                    title = stringResource(id = R.string.setting_title_meter),
                    description = stringResource(id = R.string.setting_mes_meter),
                ) {
                    Switch(
                        checked = state.isVibrateOnApproachEnabled.value,
                        onCheckedChange = state.isVibrateOnApproachEnabled.onChange,
                        enabled = state.isVibrateOnApproachEnabled.enabled,
                    )
                    NumberPicker(
                        value = state.vibrateDistanceOnApproach.value,
                        onValueChange = state.vibrateDistanceOnApproach.onChange,
                        range = (50..500 step 10).toPersistentList(),
                        enabled = state.vibrateDistanceOnApproach.enabled,
                        modifier = Modifier.width(80.dp),
                    )
                }
            }
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 16.dp,
                ),
            )
        }
        item {
            SettingGroup(
                icon = painterResource(id = R.drawable.ic_brightness),
            ) {
                SettingItem(
                    title = stringResource(id = R.string.setting_title_night),
                    description = stringResource(id = R.string.setting_mes_night),
                ) {
                    Switch(
                        checked = state.isNightMode,
                        onCheckedChange = onNightModeChanged,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(id = R.string.setting_title_night_switch),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    var expanded by remember { mutableStateOf(false) }
                    OutlinedButton(
                        modifier = Modifier.width(240.dp),
                        onClick = { expanded = true },
                        enabled = state.nightScrimTimeout.enabled,
                    ) {
                        Text(
                            text = state.nightScrimTimeout.value.label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                        )
                        Icon(
                            imageVector = Icons.Outlined.ArrowDropDown,
                            contentDescription = "select",
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        NightScrimTimeout.Values.forEach { timeout ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = timeout.label,
                                    )
                                },
                                onClick = {
                                    state.nightScrimTimeout.onChange(timeout)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                SettingItem(
                    title = stringResource(id = R.string.setting_title_brightness),
                    description = stringResource(id = R.string.setting_mes_brightness),
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp, 40.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp),
                            )
                            .clip(RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(id = R.string.setting_mes_brightness_sample),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = Color.Black.copy(
                                        alpha = 1f - state.nightScrimBrightness.value / 255f,
                                    )
                                ),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.setting_mes_brightness_dart),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(4.dp),
                    )
                    Slider(
                        value = state.nightScrimBrightness.value,
                        onValueChange = state.nightScrimBrightness.onChange,
                        valueRange = OverlayViewController.MIN_BRIGHTNESS..255f,
                        steps = (255f - OverlayViewController.MIN_BRIGHTNESS).roundToInt(),
                        enabled = state.nightScrimBrightness.enabled,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(id = R.string.setting_mes_brightness_bright),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(4.dp),
                    )
                }
            }
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 16.dp,
                ),
            )
        }
        item {
            SettingGroup(
                icon = painterResource(id = R.drawable.ic_sync),
            ) {
                DataVersionSection(
                    state = state.data,
                    checkLatestData = checkLatestData,
                )
            }
        }
    }
}

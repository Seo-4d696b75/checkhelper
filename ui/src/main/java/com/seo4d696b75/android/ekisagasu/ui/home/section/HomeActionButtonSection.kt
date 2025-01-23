package com.seo4d696b75.android.ekisagasu.ui.home.section

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.home.HomeActionButtonUiState
import com.seo4d696b75.android.ekisagasu.ui.home.component.LabelledFloatingActionButton
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme

@Composable
fun HomeActionButtonSection(
    isRunning: Boolean,
    onSearchStateChanged: () -> Unit,
    onFinishClicked: () -> Unit,
    selectLineButton: HomeActionButtonUiState,
    lineNavigatorButton: HomeActionButtonUiState,
    timerButton: HomeActionButtonUiState.Enabled,
    mapButton: HomeActionButtonUiState.Enabled,
    modifier: Modifier = Modifier,
) {
    var expand by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            horizontalAlignment = Alignment.End,
        ) {
            AnimatedVisibility(
                visible = expand,
                label = "HomeActionButtonSection expand",
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.clipToBounds(),
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    LabelledFloatingActionButton(
                        label = stringResource(id = R.string.floating_action_button_label_show_map),
                        onClick = {
                            expand = false
                            mapButton.onClick()
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_maps),
                            contentDescription = "show map",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    LabelledFloatingActionButton(
                        label = stringResource(id = R.string.floating_action_button_label_start_timer),
                        onClick = {
                            expand = false
                            timerButton.onClick()
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_timer),
                            contentDescription = "start timer",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    AnimatedVisibility(
                        visible = lineNavigatorButton is HomeActionButtonUiState.Enabled,
                    ) {
                        LabelledFloatingActionButton(
                            label = stringResource(id = R.string.floating_action_button_label_start_navigator),
                            onClick = {
                                expand = false
                                if (lineNavigatorButton is HomeActionButtonUiState.Enabled) {
                                    lineNavigatorButton.onClick()
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.line_position),
                                contentDescription = "start line navigation",
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = selectLineButton is HomeActionButtonUiState.Enabled,
                    ) {
                        LabelledFloatingActionButton(
                            label = stringResource(id = R.string.floating_action_button_label_select_line),
                            onClick = {
                                expand = false
                                if (selectLineButton is HomeActionButtonUiState.Enabled) {
                                    selectLineButton.onClick()
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_line_selects),
                                contentDescription = "select line",
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    LabelledFloatingActionButton(
                        label = stringResource(id = R.string.floating_action_button_label_finish),
                        onClick = onFinishClicked,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "finish app",
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SmallFloatingActionButton(
                    onClick = { expand = !expand },
                ) {
                    AnimatedContent(
                        targetState = expand,
                        label = "view more actions",
                        transitionSpec = {
                            if (targetState) {
                                slideInVertically { it } togetherWith slideOutVertically { -it }
                            } else {
                                slideInVertically { -it } togetherWith slideOutVertically { it }
                            }
                        },
                    ) {
                        if (it) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "close actions",
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = "more actions",
                            )
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onSearchStateChanged,
        ) {
            Crossfade(
                targetState = isRunning,
                label = "fab running",
            ) { running ->
                if (running) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pause),
                        contentDescription = "stop",
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_play),
                        contentDescription = "start",
                    )
                }
            }
        }
    }
}

@Composable
@PreviewLightDark
private fun HomeActionButtonSectionPreview() {
    AppTheme {
        Surface {
            HomeActionButtonSection(
                isRunning = true,
                onSearchStateChanged = { },
                onFinishClicked = {},
                selectLineButton = HomeActionButtonUiState.Enabled {},
                lineNavigatorButton = HomeActionButtonUiState.Enabled {},
                timerButton = HomeActionButtonUiState.Enabled {},
                mapButton = HomeActionButtonUiState.Enabled {},
                modifier = Modifier.height(320.dp),
            )
        }
    }
}

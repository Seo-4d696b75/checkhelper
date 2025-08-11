package com.seo4d696b75.android.ekisagasu.ui.navigator.section

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.common.AutoScalingText
import com.seo4d696b75.android.ekisagasu.ui.navigator.DisplayedNavigatorState
import com.seo4d696b75.android.ekisagasu.ui.navigator.DisplayedNavigatorStationState
import com.seo4d696b75.android.ekisagasu.ui.navigator.component.NavigationButton
import com.seo4d696b75.android.ekisagasu.ui.navigator.component.NavigatorInitializingSection
import com.seo4d696b75.android.ekisagasu.ui.navigator.component.NavigatorStationList
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import com.seo4d696b75.android.ekisagasu.ui.utils.previewLine
import com.seo4d696b75.android.ekisagasu.ui.utils.previewStation

@SuppressLint("UnusedCrossfadeTargetStateParameter")
@Composable
fun NavigatorSection(
    state: DisplayedNavigatorState,
    onToggle: () -> Unit,
    onStopClicked: () -> Unit,
    onSelectLineClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onToggle),
        shape = RoundedCornerShape(6.dp),
    ) {
        Row {
            Crossfade(
                targetState = state is DisplayedNavigatorState.Result,
                label = "NavigatorSection#state",
                modifier = Modifier.weight(1f),
            ) {
                when (state) {
                    DisplayedNavigatorState.Idle -> {}
                    is DisplayedNavigatorState.Initializing ->
                        NavigatorInitializingSection(
                            modifier = Modifier.fillMaxSize(),
                        )

                    is DisplayedNavigatorState.Result ->
                        NavigatorStationList(
                            currentLine = state.line,
                            stations = state.stations,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    vertical = 3.dp,
                                    horizontal = 5.dp,
                                ),
                        )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 10.dp)
                    .width(120.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
            ) {
                AutoScalingText(
                    text = state.line?.name ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(5.dp))
                Row {
                    NavigationButton(
                        onClick = onSelectLineClicked,
                        id = R.drawable.ic_line_select,
                        contentDescription = "select line",
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    NavigationButton(
                        onClick = onStopClicked,
                        id = R.drawable.ic_line_cancel,
                        contentDescription = "stop",
                    )
                }
            }
        }
    }
}

@Composable
@PreviewLightDark
private fun NavigationSectionPreview_initializing() {
    AppTheme {
        NavigatorSection(
            state = DisplayedNavigatorState.Initializing(
                previewLine.copy(name = "めっちゃ長い長い長い長い長い長い名前の路線")
            ),
            onToggle = {},
            onStopClicked = {},
            onSelectLineClicked = {},
            modifier = Modifier.height(90.dp),
        )
    }
}

@Composable
@PreviewLightDark
private fun NavigationSectionPreview_result() {
    AppTheme {
        NavigatorSection(
            state = DisplayedNavigatorState.Result(
                line = previewLine,
                stations = listOf(
                    DisplayedNavigatorStationState.Current(
                        station = previewStation,
                    ),

                    DisplayedNavigatorStationState.Prediction(
                        station = previewStation.copy(
                            id = 2,
                            code = 2,
                            name = "品川",
                            originalName = "品川",
                            nameKana = "しながわ",
                        ),
                        distance = 100f,
                    ),
                    DisplayedNavigatorStationState.Prediction(
                        station = previewStation.copy(
                            id = 3,
                            code = 3,
                            name = "新横浜",
                            originalName = "新横浜",
                            nameKana = "しんよこはま",
                            lines = listOf(previewLine),
                        ),
                        distance = 200f,
                    ),
                ),
            ),
            onToggle = {},
            onStopClicked = {},
            onSelectLineClicked = {},
            modifier = Modifier.height(87.dp),
        )
    }
}

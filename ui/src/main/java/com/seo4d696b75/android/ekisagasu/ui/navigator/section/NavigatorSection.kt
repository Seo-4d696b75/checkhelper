package com.seo4d696b75.android.ekisagasu.ui.navigator.section

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.common.AutoScalingText
import com.seo4d696b75.android.ekisagasu.ui.navigator.DisplayedNavigatorLineState
import com.seo4d696b75.android.ekisagasu.ui.navigator.DisplayedNavigatorState
import com.seo4d696b75.android.ekisagasu.ui.navigator.DisplayedNavigatorStationState
import com.seo4d696b75.android.ekisagasu.ui.navigator.component.NavigationButton
import com.seo4d696b75.android.ekisagasu.ui.navigator.component.NavigatorInitializingSection
import com.seo4d696b75.android.ekisagasu.ui.navigator.component.NavigatorStationList
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme

@SuppressLint("UnusedCrossfadeTargetStateParameter")
@Composable
fun NavigatorSection(
    state: DisplayedNavigatorState,
    onToggle: () -> Unit,
    onStopClicked: () -> Unit,
    onSelectLineClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(6.dp),
            )
            .clickable(onClick = onToggle),
    ) {
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
                    id = R.drawable.ic_line_selects,
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

val previewLine = Line(
    id = "id",
    code = 10001,
    name = "東海道新幹線",
    closed = false,
    nameKana = "とうかいどうしんかんせん",
    stationSize = 17,
    stationList = emptyArray(),
)

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
                        station = Station(
                            id = "1",
                            code = 1,
                            name = "東京",
                            originalName = "東京",
                            nameKana = "とうきょう",
                            lines = listOf(1),
                            lat = 45.5,
                            lng = 135.0,
                            prefecture = 13,
                            closed = false,
                            voronoi = "",
                            attr = "",
                        ),
                        lines = listOf(
                            DisplayedNavigatorLineState(
                                line = previewLine,
                                isCurrentSelected = true,
                            ),
                        ),
                    ),
                    DisplayedNavigatorStationState.Prediction(
                        station = Station(
                            id = "2",
                            code = 2,
                            name = "品川",
                            originalName = "品川",
                            nameKana = "しながわ",
                            lines = listOf(1),
                            lat = 45.5,
                            lng = 135.0,
                            prefecture = 13,
                            closed = false,
                            voronoi = "",
                            attr = "",
                        ),
                        lines = listOf(
                            DisplayedNavigatorLineState(
                                line = previewLine,
                                isCurrentSelected = false,
                            ),
                        ),
                        distance = 100f,
                    ),
                    DisplayedNavigatorStationState.Prediction(
                        station = Station(
                            id = "3",
                            code = 3,
                            name = "新横浜",
                            originalName = "新横浜",
                            nameKana = "しんよこはま",
                            lines = listOf(1),
                            lat = 45.5,
                            lng = 135.0,
                            prefecture = 13,
                            closed = false,
                            voronoi = "",
                            attr = "",
                        ),
                        lines = listOf(
                            DisplayedNavigatorLineState(
                                line = previewLine,
                                isCurrentSelected = false,
                            ),
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

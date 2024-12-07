package com.seo4d696b75.android.ekisagasu.ui.popup.section

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.ui.common.AutoScalingText
import com.seo4d696b75.android.ekisagasu.ui.popup.PopupStationState
import com.seo4d696b75.android.ekisagasu.ui.utils.formatDistance

@Composable
fun PopupResultSection(
    state: PopupStationState.Result,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            Row {
                AutoScalingText(
                    text = state.nearest.station.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.alignByBaseline(),
                )
                Spacer(modifier = Modifier.width(4.dp))
                if (state.showPrefecture) {
                    Text(
                        text = state.nearest.station.prefecture.name,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
            Row {
                Crossfade(
                    targetState = state.nearest.distance,
                    label = "PopupSection distance",
                ) { distance ->
                    Text(
                        text = distance.formatDistance,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                AutoScalingText(
                    text = state.nearest.station.getLinesName(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        Crossfade(
            targetState = state.time,
            label = "PopupSection time",
        ) { time ->
            Text(
                text = time.text(),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .padding(
                        start = 10.dp,
                        end = 6.dp,
                        top = 3.dp,
                    )
                    .fillMaxHeight(),
            )
        }
    }
}

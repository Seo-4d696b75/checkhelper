package com.seo4d696b75.android.ekisagasu.ui.station.section

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.common.AutoScalingText
import com.seo4d696b75.android.ekisagasu.ui.common.LineColorTile
import com.seo4d696b75.android.ekisagasu.ui.common.SquareIconButton
import com.seo4d696b75.android.ekisagasu.ui.common.StationName
import com.seo4d696b75.android.ekisagasu.ui.station.StationUiState
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import com.seo4d696b75.android.ekisagasu.ui.utils.previewLines
import com.seo4d696b75.android.ekisagasu.ui.utils.previewStation

@Composable
fun StationDetailSection(
    state: StationUiState.Loaded,
    onClose: () -> Unit,
    onMapClicked: () -> Unit,
    onLineClicked: (Line) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
    ) {
        Box(
            contentAlignment = Alignment.TopEnd,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.title_detail_station),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                StationName(
                    station = state.station,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_train),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row {
                            Text(text = stringResource(id = R.string.title_detail_station_prefecture))
                            Text(text = state.station.prefecture.name)
                        }
                        Row {
                            Text(text = stringResource(id = R.string.title_detail_station_location))
                            Text(text = "E%.6f N%.6f".format(state.station.lng, state.station.lat))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.title_line_list),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(
                        items = state.lines,
                        key = { it.code },
                    ) { line ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLineClicked(line) }
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LineColorTile(
                                line = line,
                                modifier = Modifier.size(20.dp),
                            )
                            AutoScalingText(
                                text = line.name,
                                modifier = Modifier.widthIn(max = 150.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = stringResource(id = R.string.message_station_size, line.stationSize),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.padding(8.dp),
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "close station detail",
                        modifier = Modifier.size(48.dp),
                    )
                }
                SquareIconButton(onClick = onMapClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_map),
                        contentDescription = "show station on map",
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
        }
    }
}

@Composable
@PreviewLightDark
private fun StationDetailSectionPreview() {
    AppTheme {
        StationDetailSection(
            state = StationUiState.Loaded(
                station = previewStation,
                lines = previewLines,
            ),
            onClose = {},
            onMapClicked = {},
            onLineClicked = {},
        )
    }
}

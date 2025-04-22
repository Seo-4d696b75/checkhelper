package com.seo4d696b75.android.ekisagasu.ui.line.section

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
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.common.LineColorTile
import com.seo4d696b75.android.ekisagasu.ui.common.LineName
import com.seo4d696b75.android.ekisagasu.ui.common.SquareIconButton
import com.seo4d696b75.android.ekisagasu.ui.line.LineUiState
import com.seo4d696b75.android.ekisagasu.ui.line.StationRegistrationUiState
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import com.seo4d696b75.android.ekisagasu.ui.utils.previewLine
import com.seo4d696b75.android.ekisagasu.ui.utils.previewStations

@Composable
fun LineDetailSection(
    state: LineUiState.Loaded,
    onClose: () -> Unit,
    onMapClicked: () -> Unit,
    onStationClicked: (Station) -> Unit,
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
                    text = stringResource(id = R.string.title_detail_line),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LineColorTile(
                        line = state.line,
                        modifier = Modifier.size(32.dp),
                    )
                    LineName(line = state.line)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.title_detail_list_station),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(
                        items = state.stations,
                        key = { it.code },
                    ) { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStationClicked(r.station) }
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (r.numbering.isNotEmpty()) {
                                Text(
                                    text = r.numbering,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.alignByBaseline(),
                                )
                            }
                            Text(
                                text = r.station.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.alignByBaseline(),
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
                        contentDescription = "close line detail",
                        modifier = Modifier.size(48.dp),
                    )
                }
                SquareIconButton(onClick = onMapClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_map),
                        contentDescription = "show line on map",
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
        }
    }
}

@Composable
@PreviewLightDark
private fun LineDetailSectionPreview() {
    AppTheme {
        LineDetailSection(
            state = LineUiState.Loaded(
                line = previewLine,
                stations = previewStations.map {
                    StationRegistrationUiState(it.code, it, "N${it.code}")
                },
            ),
            onClose = { },
            onMapClicked = { },
            onStationClicked = {},
        )
    }
}

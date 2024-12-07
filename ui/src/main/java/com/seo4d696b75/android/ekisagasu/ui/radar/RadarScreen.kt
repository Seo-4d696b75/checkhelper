package com.seo4d696b75.android.ekisagasu.ui.radar

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.domain.search.NearStation
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.radar.section.RadarListSection
import com.seo4d696b75.android.ekisagasu.ui.radar.section.RadarLoadingSection
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import com.seo4d696b75.android.ekisagasu.ui.utils.previewLines
import com.seo4d696b75.android.ekisagasu.ui.utils.previewStation
import java.util.Date

@Composable
fun RadarScreen(
    onStationClicked: (Station) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RadarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    RadarScreen(
        state = state,
        onStationClicked = onStationClicked,
        modifier = modifier,
    )
}

@Composable
fun RadarScreen(
    state: RadarUiState,
    onStationClicked: (Station) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is RadarUiState.Running ->
            Column(
                modifier = modifier.fillMaxSize(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.radar),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = "✕${state.size}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                when (state) {
                    is RadarUiState.Initializing ->
                        RadarLoadingSection(size = state.size)

                    is RadarUiState.Data ->
                        RadarListSection(
                            list = state.list,
                            onStationClicked = onStationClicked,
                        )
                }
            }

        else -> {}
    }
}

private class RadarScreenPreviewParamProvider : PreviewParameterProvider<RadarUiState> {
    override val values = sequenceOf(
        RadarUiState.None,
        RadarUiState.Initializing(12),
        RadarUiState.Data(
            size = 12,
            list = List(12) { index ->
                NearStation(
                    station = previewStation.copy(
                        id = "id-$index",
                        code = index,
                        lines = previewLines.take(index + 1),
                    ),
                    distance = (index + 1) * 100f,
                    time = Date(),
                )
            },
        ),
    )
}

@Composable
@PreviewLightDark
private fun PreviewRadarScreen(
    @PreviewParameter(RadarScreenPreviewParamProvider::class)
    state: RadarUiState,
) {
    AppTheme {
        Surface {
            RadarScreen(
                state = state,
                onStationClicked = {},
            )
        }
    }
}

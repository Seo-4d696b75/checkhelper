package com.seo4d696b75.android.ekisagasu.ui.home.section

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.common.AutoScalingText
import com.seo4d696b75.android.ekisagasu.ui.home.HomeUiState
import com.seo4d696b75.android.ekisagasu.ui.home.component.SearchIndicator
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import com.seo4d696b75.android.ekisagasu.ui.utils.previewLine
import com.seo4d696b75.android.ekisagasu.ui.utils.previewNearStation

@SuppressLint("UnusedCrossfadeTargetStateParameter")
@Composable
fun HomeSection(
    state: HomeUiState.Visible,
    onStationClicked: (Station) -> Unit,
    onLineClicked: (Line) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        HomeResultSection(
            state = state,
            onStationClicked = onStationClicked,
            onLineClicked = onLineClicked,
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
        )
        Column(
            horizontalAlignment = Alignment.End,
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 2.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_line_select),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                AutoScalingText(
                    text = when (state) {
                        is HomeUiState.Result -> state.selectedLine?.name
                        else -> null
                    } ?: stringResource(id = R.string.no_selected_line),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            SearchIndicator(
                running = state is HomeUiState.Running,
                modifier = Modifier
                    .padding(end = 5.dp)
                    .size(100.dp),
            )
        }
    }
}

private class HomeSectionPreviewParamProvider : PreviewParameterProvider<HomeUiState.Visible> {
    override val values = sequenceOf(
        HomeUiState.Idle,
        HomeUiState.Initializing,
        HomeUiState.Result(
            station = previewNearStation,
            selectedLine = previewLine,
        ),
    )
}

@Composable
@PreviewLightDark
private fun HomeSectionPreview(
    @PreviewParameter(HomeSectionPreviewParamProvider::class)
    state: HomeUiState.Visible,
) {
    AppTheme {
        Surface {
            HomeSection(
                state = state,
                onStationClicked = {},
                onLineClicked = {},
            )
        }
    }
}

package com.seo4d696b75.android.ekisagasu.ui.radar.section

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.domain.search.NearStation
import com.seo4d696b75.android.ekisagasu.ui.common.AutoScalingText
import com.seo4d696b75.android.ekisagasu.ui.utils.formatDistance

@Composable
fun RadarListSection(
    list: List<NearStation>,
    onStationClicked: (Station) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        itemsIndexed(
            items = list,
            key = { _, n -> n.station.code },
        ) { index, n ->
            Column(
                modifier = Modifier.animateItem(),
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onStationClicked(n.station) }
                        .padding(4.dp),
                ) {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(16.dp)
                            .alignByBaseline(),
                    )
                    Text(
                        text = n.distance.formatDistance,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(50.dp)
                            .alignByBaseline(),
                    )
                    AutoScalingText(
                        text = n.station.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .widthIn(max = 150.dp)
                            .alignByBaseline(),
                    )
                    AutoScalingText(
                        text = n.station.getLinesName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .alignByBaseline()
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

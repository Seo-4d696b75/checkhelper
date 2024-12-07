package com.seo4d696b75.android.ekisagasu.ui.home.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.domain.search.NearStation
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.common.StationName
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import com.seo4d696b75.android.ekisagasu.ui.utils.asComposeColor
import com.seo4d696b75.android.ekisagasu.ui.utils.formatDistance
import com.seo4d696b75.android.ekisagasu.ui.utils.parseColorCode
import com.seo4d696b75.android.ekisagasu.ui.utils.previewNearStation

@Composable
fun HomeResultGrid(
    station: NearStation,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        StationName(
            station = station.station,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_location),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = station.distance.formatDistance,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .alignByBaseline(),
            )
            Text(
                text = station.station.prefecture.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .alignByBaseline(),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.station),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.size(24.dp),
            )
            LazyRow(
                modifier = Modifier.padding(start = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(
                    items = station.station.lines,
                    key = { it.code },
                ) { line ->
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(
                                    color = parseColorCode(line.color).asComposeColor(),
                                    shape = RoundedCornerShape(4.dp),
                                ),
                        )
                        Text(
                            text = line.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
@PreviewLightDark
private fun HomeResultGridPreview() {
    AppTheme {
        Surface {
            HomeResultGrid(
                station = previewNearStation,
                modifier = Modifier.width(200.dp),
            )
        }
    }
}

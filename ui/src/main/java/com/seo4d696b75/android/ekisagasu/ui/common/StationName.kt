package com.seo4d696b75.android.ekisagasu.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme

@Composable
fun StationName(
    station: Station,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AutoScalingText(
            text = station.name,
            style = MaterialTheme.typography.titleLarge,
        )
        AutoScalingText(
            text = station.nameKana,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

val previewStation = Station(
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
)

@Composable
@PreviewLightDark
private fun StationNamePreview() {
    AppTheme {
        Surface {
            Column(
                modifier = Modifier
                    .width(100.dp)
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StationName(
                    station = previewStation,
                )
                StationName(
                    station = previewStation.copy(
                        name = "本郷三丁目",
                        nameKana = "ほんごうさんちょうめ",
                    ),
                )
                StationName(
                    station = previewStation.copy(
                        name = "等持院・立命館大学衣笠キャンパス前駅",
                        nameKana = "とうじいん・りつめいかんだいがくきぬがさキャンパスまええき",
                    ),
                )
            }
        }
    }
}

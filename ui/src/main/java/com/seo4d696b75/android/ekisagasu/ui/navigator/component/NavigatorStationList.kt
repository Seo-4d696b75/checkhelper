package com.seo4d696b75.android.ekisagasu.ui.navigator.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.common.AutoScalingText
import com.seo4d696b75.android.ekisagasu.ui.navigator.DisplayedNavigatorStationState
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import com.seo4d696b75.android.ekisagasu.ui.utils.formatDistance
import com.seo4d696b75.android.ekisagasu.ui.utils.previewLine
import com.seo4d696b75.android.ekisagasu.ui.utils.previewStation

@Composable
fun NavigatorStationList(
    currentLine: Line,
    stations: List<DisplayedNavigatorStationState>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        Image(
            painter = painterResource(id = R.drawable.navigator_anim_background),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .padding(start = 46.dp)
                .width(40.dp)
                .fillMaxHeight(),
        )
        LazyColumn(
            reverseLayout = true,
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 1.5.dp),
        ) {
            items(
                items = stations,
                // 現在駅 or 予測駅の区別は無視して同じ駅なら同じリスト要素と見なす
                key = { item -> item.station.code },
            ) { item ->
                Row(
                    modifier = Modifier
                        .height(25.dp)
                        .padding(vertical = 3.dp)
                        .animateItem(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val distance = when (item) {
                        is DisplayedNavigatorStationState.Prediction -> item.distance.formatDistance
                        else -> ""
                    }
                    Crossfade(
                        targetState = distance,
                        label = "NavigatorStationList ${item.station.code}",
                    ) { value ->
                        AutoScalingText(
                            text = value,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(46.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .scale(scaleY = 0.75f, scaleX = 1f)
                            .background(
                                color = Color.White,
                                shape = RoundedCornerShape(50),
                            ),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    AutoScalingText(
                        text = item.station.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 14.sp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AutoScalingText(
                        text = buildAnnotatedString {
                            val sorted = item.station.lines.sortedBy {
                                if (it == currentLine) 0 else 1
                            }
                            val iterator = sorted.iterator()
                            while (iterator.hasNext()) {
                                val line = iterator.next()
                                withStyle(
                                    style = SpanStyle(
                                        color = if (line == currentLine) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.Unspecified
                                        },
                                    ),
                                ) {
                                    append(line.name)
                                }
                                if (iterator.hasNext()) {
                                    append(" ")
                                }
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Image(
            painter = painterResource(id = R.drawable.navigator_anim_foreground),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .padding(start = 46.dp)
                .width(40.dp)
                .fillMaxHeight(),
        )
    }
}

@Composable
@PreviewLightDark
private fun NavigatorStationListPreview() {
    AppTheme {
        Surface {
            NavigatorStationList(
                currentLine = previewLine,
                stations = listOf(
                    DisplayedNavigatorStationState.Current(
                        station = previewStation,
                    ),
                    DisplayedNavigatorStationState.Prediction(
                        station = previewStation.copy(
                            id = "2",
                            code = 2,
                            name = "品川",
                            originalName = "品川",
                            nameKana = "しながわ",
                        ),
                        distance = 100f,
                    ),
                    DisplayedNavigatorStationState.Prediction(
                        station = previewStation.copy(
                            id = "3",
                            code = 3,
                            name = "新横浜",
                            originalName = "新横浜",
                            nameKana = "しんよこはま",
                            lines = listOf(previewLine),
                        ),
                        distance = 200f,
                    ),
                ),
                modifier = Modifier.height(81.dp),
            )
        }
    }
}

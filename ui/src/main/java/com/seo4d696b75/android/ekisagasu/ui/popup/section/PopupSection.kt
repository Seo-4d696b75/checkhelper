package com.seo4d696b75.android.ekisagasu.ui.popup.section

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.domain.search.NearStation
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.popup.PopupStationState
import com.seo4d696b75.android.ekisagasu.ui.popup.component.StationDetectedTime
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import com.seo4d696b75.android.ekisagasu.ui.utils.previewStation
import java.util.Date

@SuppressLint("UnusedCrossfadeTargetStateParameter")
@Composable
fun PopupSection(
    state: PopupStationState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_app),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .padding(start = 8.dp, end = 10.dp)
                    .size(36.dp),
            )
            val station = when (state) {
                PopupStationState.None -> null
                is PopupStationState.Result -> state.nearest.station
            }
            Crossfade(
                targetState = station,
                label = "PopupSection station",
            ) {
                if (state is PopupStationState.Result) {
                    PopupResultSection(state)
                }
            }
        }
    }
}

@Composable
@PreviewLightDark
private fun PopupSectionPreview() {
    AppTheme {
        PopupSection(
            state = PopupStationState.Result(
                time = StationDetectedTime.Now,
                nearest = NearStation(
                    station = previewStation,
                    distance = 176f,
                    time = Date(),
                ),
                showPrefecture = true,
            ),
            onClick = {},
            modifier = Modifier.height(53.dp),
        )
    }
}

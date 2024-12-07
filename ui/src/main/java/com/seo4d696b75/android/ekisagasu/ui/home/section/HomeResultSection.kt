package com.seo4d696b75.android.ekisagasu.ui.home.section

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.home.HomeUiState
import com.seo4d696b75.android.ekisagasu.ui.home.component.HomeResultGrid

@SuppressLint("UnusedCrossfadeTargetStateParameter")
@Composable
fun HomeResultSection(
    state: HomeUiState,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = state.javaClass,
        label = "HomeSection state",
        modifier = modifier,
    ) {
        when (state) {
            HomeUiState.Idle ->
                Text(
                    text = stringResource(id = R.string.main_message_wait_search),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .padding(horizontal = 8.dp),
                )

            HomeUiState.Initializing ->
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(id = R.string.main_message_starting_search),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(5.dp)
                    )
                }

            is HomeUiState.Result ->
                HomeResultGrid(
                    station = state.station,
                    modifier = Modifier.padding(end = 5.dp),
                )
        }
    }
}

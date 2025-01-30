package com.seo4d696b75.android.ekisagasu.ui.selectLine.section

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.common.AutoScalingText
import com.seo4d696b75.android.ekisagasu.ui.common.LineColorTile
import kotlinx.collections.immutable.ImmutableList

@Composable
fun LineSelectSection(
    message: String?,
    lines: ImmutableList<Line>,
    onLineSelected: (Line) -> Unit,
) {
    Column {
        Text(text = message ?: "")
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
        ) {
            items(
                items = lines,
                key = { it.code },
            ) { line ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLineSelected(line) }
                        .padding(4.dp),
                ) {
                    LineColorTile(
                        line = line,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    AutoScalingText(
                        text = line.name,
                        maxLines = 1,
                        modifier = Modifier.widthIn(max = 150.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(id = R.string.message_station_size, line.stationSize),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}

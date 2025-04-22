package com.seo4d696b75.android.ekisagasu.ui.log.section

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.domain.date.TIME_PATTERN_MILLI_SEC
import com.seo4d696b75.android.ekisagasu.domain.date.format
import com.seo4d696b75.android.ekisagasu.domain.log.AppLog
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogType
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import java.util.Date

@Composable
fun LogListSection(
    logs: ImmutableList<AppLog>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        items(
            items = logs,
            key = { it.id },
        ) { log ->
            Column {
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Text(
                        text = log.timestamp.format(TIME_PATTERN_MILLI_SEC),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = log.message,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
@PreviewLightDark
private fun LogListSectionPreview() {
    AppTheme {
        Surface {
            LogListSection(
                logs = List(20) {
                    AppLog(
                        id = it.toLong(),
                        type = AppLogType.System,
                        message = "log message",
                        timestamp = Date(),
                    )
                }.toPersistentList(),
            )
        }
    }
}

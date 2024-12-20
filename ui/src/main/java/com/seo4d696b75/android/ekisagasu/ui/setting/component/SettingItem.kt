package com.seo4d696b75.android.ekisagasu.ui.setting.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.ui.common.M3NumberPicker
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme

@Composable
fun SettingItem(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Column(
            modifier = Modifier.width(120.dp),
            content = content,
            horizontalAlignment = Alignment.CenterHorizontally,
        )
    }
}

@Composable
@PreviewLightDark
private fun SettingItemPreview() {
    AppTheme {
        Surface {
            SettingItem(
                title = "設定タイトル",
                description = "設定の細かい説明文",
            ) {
                var value by remember {
                    mutableIntStateOf(1)
                }
                M3NumberPicker(
                    value = value,
                    onValueChange = { value = it },
                    range = 1..10,
                    modifier = Modifier.width(70.dp),
                )
            }
        }
    }
}

package com.seo4d696b75.android.ekisagasu.ui.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme

@Composable
fun LabelledFloatingActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceDim,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(
                    horizontal = 8.dp,
                    vertical = 4.dp,
                ),
        )
        Spacer(modifier = Modifier.width(8.dp))
        SmallFloatingActionButton(
            onClick = onClick,
            content = content,
        )
    }
}

@Composable
@PreviewLightDark
private fun LabelledFloatingActionButtonPreview() {
    AppTheme {
        Surface {
            LabelledFloatingActionButton(
                label = "label",
                onClick = { },
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                )
            }
        }
    }
}

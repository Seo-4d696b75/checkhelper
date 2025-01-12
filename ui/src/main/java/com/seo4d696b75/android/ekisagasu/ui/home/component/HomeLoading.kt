package com.seo4d696b75.android.ekisagasu.ui.home.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.common.AutoScalingText
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import com.valentinilk.shimmer.shimmer

@Composable
fun HomeLoading(
    modifier: Modifier = Modifier,
) {
    val shimmer = Modifier
        .shimmer()
        .background(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    Column(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .then(shimmer),
        ) {
            Column {
                AutoScalingText(
                    text = "",
                    style = MaterialTheme.typography.headlineMedium,
                )
                AutoScalingText(
                    text = "",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
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
                text = "",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .width(120.dp)
                    .then(shimmer),
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
            Text(
                text = "",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .fillMaxWidth()
                    .then(shimmer),
            )
        }
    }
}

@Composable
@PreviewLightDark
private fun PreviewHomeLoading() {
    AppTheme {
        Surface {
            HomeLoading(
                modifier = Modifier.width(200.dp),
            )
        }
    }
}

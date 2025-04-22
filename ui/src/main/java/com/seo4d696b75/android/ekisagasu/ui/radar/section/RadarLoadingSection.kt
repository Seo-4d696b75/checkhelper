package com.seo4d696b75.android.ekisagasu.ui.radar.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.shimmer

@Composable
fun RadarLoadingSection(
    size: Int,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        items(size) {
            Box(
                modifier = Modifier
                    .shimmer()
                    .padding(4.dp)
                    .fillMaxWidth()
                    .background(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceDim,
                    ),
            ) {
                Text(
                    text = "",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

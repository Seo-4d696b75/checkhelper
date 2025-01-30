package com.seo4d696b75.android.ekisagasu.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.ui.utils.asComposeColor
import com.seo4d696b75.android.ekisagasu.ui.utils.parseColorCode

@Composable
fun LineColorTile(
    line: Line,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(
            color = parseColorCode(line.color).asComposeColor(),
            shape = RoundedCornerShape(4.dp),
        )
    )
}

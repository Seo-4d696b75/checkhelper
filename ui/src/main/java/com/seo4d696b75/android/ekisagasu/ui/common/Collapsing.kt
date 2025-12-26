package com.seo4d696b75.android.ekisagasu.ui.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Collapsing(
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = content,
    ) { measurables, constraints ->
        require(measurables.size == 1)
        val placeable = measurables.first().measure(constraints)

        val state = scrollBehavior.state
        val contentHeight = placeable.height

        // 参考：https://android.googlesource.com/platform/frameworks/support/+/03bd1b71ce768b0d243f16ca53c87de61960719b/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/AppBar.kt#1641
        if (state.heightOffsetLimit != -contentHeight.toFloat()) {
            state.heightOffsetLimit = -contentHeight.toFloat()
        }

        // 参考： https://android.googlesource.com/platform/frameworks/support/+/03bd1b71ce768b0d243f16ca53c87de61960719b/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/AppBar.kt#1692
        val layoutHeight = contentHeight + state.heightOffset

        // スクロールされた分を覗いた高さ分を領域とする
        layout(constraints.maxWidth, layoutHeight.toInt().coerceAtLeast(0)) {
            // オフセット分下に描画する
            placeable.placeRelative(
                x = 0,
                y = state.heightOffset.toInt(),
            )
        }
    }
}

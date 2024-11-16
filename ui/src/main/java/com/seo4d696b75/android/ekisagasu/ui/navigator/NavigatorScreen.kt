package com.seo4d696b75.android.ekisagasu.ui.navigator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.ui.common.OverlayAppIcon
import com.seo4d696b75.android.ekisagasu.ui.navigator.section.NavigatorSection
import com.seo4d696b75.android.ekisagasu.ui.navigator.section.previewLine
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme

@Composable
fun NavigatorScreen(
    uiState: NavigatorUiState,
    onToggle: () -> Unit,
    onStopClicked: () -> Unit,
    onSelectLineClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.TopStart,
        modifier = modifier.then(
            if (!uiState.visible) {
                Modifier
            } else if (uiState.isExpanded) {
                // View 全体のサイズを変更する関係で
                // アニメーション中の OverlayAppIcon を左寄せに保つため必要
                Modifier
                    .height(90.dp)
                    .fillMaxWidth()
            } else {
                Modifier.size(59.dp)
            }
        )
    ) {
        AnimatedVisibility(
            visible = uiState.visible,
            label = "OverlayExpandable#isVisible",
            enter = slideInVertically { -it + 10 },
            exit = slideOutVertically { -it + 10 },
        ) {
            val density = LocalDensity.current
            AnimatedContent(
                targetState = uiState.isExpanded,
                label = "OverlayExpandable#isExpanded",
                transitionSpec = {
                    val iconSize = with(density) {
                        val size = 59.dp.roundToPx()
                        IntSize(size, size)
                    }
                    if (targetState) {
                        // 開く時
                        fadeIn() + expandIn(
                            expandFrom = Alignment.TopStart,
                            initialSize = { iconSize },
                        ) togetherWith fadeOut()
                    } else {
                        // 閉じる時
                        fadeIn() togetherWith shrinkOut(
                            shrinkTowards = Alignment.TopStart,
                            targetSize = { iconSize },
                        ) + fadeOut()
                    }
                },
            ) { expand ->
                if (expand) {
                    NavigatorSection(
                        state = uiState.navigation,
                        onToggle = onToggle,
                        onStopClicked = onStopClicked,
                        onSelectLineClicked = onSelectLineClicked,
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .padding(top = 3.dp)
                            .height(87.dp)
                            .fillMaxWidth()
                    )
                } else {
                    OverlayAppIcon(
                        onClick = onToggle,
                        modifier = Modifier
                            .padding(3.dp)
                            .size(53.dp),
                    )
                }
            }
        }
    }
}

@Composable
@Preview
private fun NavigatorScreenPreview_visible() {
    AppTheme {
        var isVisible by remember { mutableStateOf(true) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(MaterialTheme.colorScheme.inverseSurface)
                .clickable { isVisible = !isVisible },
        ) {
            NavigatorScreen(
                uiState = NavigatorUiState(
                    visible = isVisible,
                    isExpanded = true,
                    navigation = DisplayedNavigatorState.Initializing(
                        line = previewLine,
                    ),
                ),
                onToggle = { isVisible = false },
                onStopClicked = {},
                onSelectLineClicked = {},
            )
        }
    }
}

@Composable
@Preview
private fun NavigatorScreenPreview_expand() {
    AppTheme {
        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.inverseSurface),
        ) {
            var isExpanded by remember { mutableStateOf(true) }
            NavigatorScreen(
                uiState = NavigatorUiState(
                    visible = true,
                    isExpanded = isExpanded,
                    navigation = DisplayedNavigatorState.Initializing(
                        line = previewLine,
                    ),
                ),
                onToggle = { isExpanded = !isExpanded },
                onStopClicked = {},
                onSelectLineClicked = {},
            )
        }
    }
}

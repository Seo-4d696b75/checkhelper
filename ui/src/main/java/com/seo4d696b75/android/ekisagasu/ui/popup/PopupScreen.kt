package com.seo4d696b75.android.ekisagasu.ui.popup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.ui.common.OverlayAppIcon
import com.seo4d696b75.android.ekisagasu.ui.popup.section.PopupSection

@Composable
fun PopupScreen(
    uiState: PopupUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.TopStart,
        modifier = modifier.then(
            if (uiState.isExpanded) {
                // View 全体のサイズを変更する関係で
                // アニメーション中の OverlayAppIcon を左寄せに保つため必要
                Modifier.fillMaxWidth()
            } else {
                Modifier.wrapContentWidth()
            }
        ),
    ) {
        AnimatedVisibility(
            visible = uiState.visible,
            label = "OverlayExpandable#isVisible",
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
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
                    PopupSection(
                        state = uiState.current,
                        onClick = onClick,
                        modifier = Modifier
                            .padding(3.dp)
                            .height(53.dp)
                            .fillMaxWidth(),
                    )
                } else {
                    OverlayAppIcon(
                        onClick = onClick,
                        modifier = Modifier
                            .padding(3.dp)
                            .size(53.dp),
                    )
                }
            }
        }
    }
}

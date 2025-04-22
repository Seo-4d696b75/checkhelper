package com.seo4d696b75.android.ekisagasu.ui.home.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun SearchIndicator(
    running: Boolean,
    modifier: Modifier = Modifier,
) {
    var shouldRunning by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(running) {
        if (running) {
            // アニメーション開始
            shouldRunning = true
        }
    }

    var degree by remember {
        mutableFloatStateOf(0f)
    }

    LaunchedEffect(shouldRunning) {
        if (shouldRunning) {
            animate(
                initialValue = degree,
                targetValue = degree + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 360 * 100,
                        easing = LinearEasing,
                    ),
                ),
            ) { value, _ ->
                degree = value
            }
        }
    }

    // アニメーション終了は区切りの良いタイミングに合わせる
    LaunchedEffect(shouldRunning, running) {
        if (shouldRunning && !running) {
            snapshotFlow { degree }
                .map { floor(it * 16 / 360).roundToInt() }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    shouldRunning = false
                    degree = 360f / 16 * it.mod(16)
                }
        }
    }

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.loop_line_background),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = -degree
                },
        )
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_app),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier.size(maxWidth * 0.6f),
            )
        }
    }
}

@Composable
@PreviewLightDark
private fun SearchIndicatorPreview() {
    AppTheme {
        Surface {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                var running by remember {
                    mutableStateOf(false)
                }
                Button(
                    onClick = { running = !running },
                ) {
                    Text(
                        text = if (running) "stop" else "start",
                    )
                }
                SearchIndicator(
                    running = running,
                    modifier = Modifier.size(160.dp),
                )
            }
        }
    }
}

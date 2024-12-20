package com.seo4d696b75.android.ekisagasu.ui.common

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.math.absoluteValue

@Composable
fun M3NumberPicker(
    value: Int,
    range: Iterable<Int>,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: PickerColors = PickerDefaults.colors(),
    labelStyle: TextStyle = PickerDefaults.labelStyle(),
    itemSize: DpSize = PickerDefaults.itemSize,
    dividerHeight: Dp = PickerDefaults.dividerHeight,
) {
    M3Picker(
        value = value,
        values = range.toList(),
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        labelStyle = labelStyle,
        itemSize = itemSize,
        dividerHeight = dividerHeight,
    )
}

@Composable
fun <T> M3Picker(
    value: T,
    values: List<T>,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: PickerColors = PickerDefaults.colors(),
    labelStyle: TextStyle = PickerDefaults.labelStyle(),
    itemSize: DpSize = PickerDefaults.itemSize,
    dividerHeight: Dp = PickerDefaults.dividerHeight,
) {
    M3Picker(
        modifier = modifier,
        value = value,
        values = values,
        onValueChange = onValueChange,
        enabled = enabled,
        colors = colors,
        itemSize = itemSize,
        dividerHeight = dividerHeight,
    ) {
        Text(
            text = it.toString(),
            style = labelStyle,
        )
    }
}

@Composable
fun <T> M3Picker(
    value: T,
    values: List<T>,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: PickerColors = PickerDefaults.colors(),
    itemSize: DpSize = PickerDefaults.itemSize,
    dividerHeight: Dp = PickerDefaults.dividerHeight,
    snapAnimationSpec: AnimationSpec<Float> = PickerDefaults.snapAnimationSpec,
    decayAnimationSpec: DecayAnimationSpec<Float> = PickerDefaults.decayAnimationSpec,
    snapDistance: PagerSnapDistance = PickerDefaults.snapDistance,
    label: @Composable (T) -> Unit,
) {
    val minAlpha = 0.3f

    val index = values.indexOf(value)
    val pagerState = rememberPagerState(
        initialPage = index,
        pageCount = { values.size },
    )

    LaunchedEffect(pagerState, index) {
        if (pagerState.currentPage != index) {
            pagerState.scrollToPage(index)
        }
    }

    val fling = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = snapAnimationSpec,
        decayAnimationSpec = decayAnimationSpec,
        pagerSnapDistance = snapDistance,
        snapPositionalThreshold = 0.5f,
    )

    val callback by rememberUpdatedState(onValueChange)
    LaunchedEffect(pagerState, values) {
        snapshotFlow { pagerState.settledPage to pagerState.isScrollInProgress }
            .filter { !it.second }
            .map { it.first }
            .collect {
                callback(values[it])
            }
    }

    Box(
        modifier = modifier.size(
            width = itemSize.width,
            height = itemSize.height * 3 + dividerHeight * 2,
        ),
    ) {
        VerticalPager(
            state = pagerState,
            flingBehavior = fling,
            pageSpacing = dividerHeight,
            contentPadding = PaddingValues(vertical = itemSize.height + dividerHeight),
            userScrollEnabled = enabled,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val alpha by remember {
                derivedStateOf {
                    val pageFloat = pagerState.currentPage + pagerState.currentPageOffsetFraction
                    val distance = (page - pageFloat).absoluteValue.coerceAtMost(1f)
                    1f - (1f - minAlpha) * distance
                }
            }
            Box(
                modifier = Modifier
                    .size(itemSize)
                    .alpha(alpha),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides colors.contentColor(enabled)
                ) {
                    label(values[page])
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Spacer(modifier = Modifier.height(itemSize.height))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dividerHeight)
                    .background(colors.dividerColor(enabled))
            )
            Spacer(modifier = Modifier.height(itemSize.height))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dividerHeight)
                    .background(colors.dividerColor(enabled))
            )
            Spacer(modifier = Modifier.height(itemSize.height))
        }
    }
}

@Immutable
class PickerColors(
    val contentColor: Color,
    val dividersColor: Color,
    val disabledContentColor: Color,
    val disabledDividerColor: Color,
) {
    @Stable
    fun contentColor(enabled: Boolean) = if (enabled) {
        contentColor
    } else {
        disabledContentColor
    }

    @Stable
    fun dividerColor(enabled: Boolean) = if (enabled) {
        dividersColor
    } else {
        disabledDividerColor
    }
}

@Stable
object PickerDefaults {
    @Composable
    fun colors(
        // see M3 Date pickers > Enabled > Label text
        contentColor: Color = MaterialTheme.colorScheme.onSurface,
        // custom color
        dividersColor: Color = MaterialTheme.colorScheme.primary,
        // see M3 Date pickers > Disabled > Label text
        disabledContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        // see M3 Switch > Disabled > Track
        disabledDividerColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    ): PickerColors = PickerColors(
        contentColor = contentColor,
        dividersColor = dividersColor,
        disabledContentColor = disabledContentColor,
        disabledDividerColor = disabledDividerColor,
    )

    @Composable
    fun labelStyle() = LocalTextStyle.current.copy(color = Color.Unspecified)

    val itemSize = DpSize(80.dp, 48.dp)

    val dividerHeight = 2.dp

    val snapAnimationSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow)

    val decayAnimationSpec: DecayAnimationSpec<Float>
        @Composable
        get() = rememberSplineBasedDecay()

    /**
     * Default fling behavior param.
     *
     * Returns the suggested target page which is calculated from `velocity` with `decayAnimationSpec`.
     *
     * @see [PagerDefaults.flingBehavior]
     */
    val snapDistance = object : PagerSnapDistance {
        override fun calculateTargetPage(
            startPage: Int,
            suggestedTargetPage: Int,
            velocity: Float,
            pageSize: Int,
            pageSpacing: Int
        ) = suggestedTargetPage
    }
}

@Composable
@PreviewLightDark
private fun M3NumberPickerPreview() {
    AppTheme {
        Surface {
            var value by remember {
                mutableIntStateOf(1)
            }
            Row {
                M3NumberPicker(
                    value = value,
                    onValueChange = { value = it },
                    range = 1..10,
                    enabled = true,
                    modifier = Modifier
                        .width(80.dp)
                        .padding(10.dp),
                )
                M3NumberPicker(
                    value = value,
                    onValueChange = { value = it },
                    range = 1..10,
                    enabled = false,
                    modifier = Modifier
                        .width(80.dp)
                        .padding(10.dp),
                )
            }
        }
    }
}

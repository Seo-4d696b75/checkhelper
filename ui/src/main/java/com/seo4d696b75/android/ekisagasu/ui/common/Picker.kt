package com.seo4d696b75.android.ekisagasu.ui.common

import androidx.annotation.FloatRange
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

    val labelSize = DpSize(80.dp, 48.dp)

    val dividerHeight = 2.dp

    @Composable
    fun flingBehavior(
        state: PickerState<Any>,
        flingEnabled: Boolean = true,
        decayAnimationSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay(),
        snapAnimationSpec: AnimationSpec<Float> = spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = Int.VisibilityThreshold.toFloat(),
        ),
    ): TargetedFlingBehavior = remember(state, flingEnabled, decayAnimationSpec, snapAnimationSpec) {
        snapFlingBehavior(
            snapLayoutInfoProvider = PickerSnapLayoutProvider(state, flingEnabled),
            decayAnimationSpec = decayAnimationSpec,
            snapAnimationSpec = snapAnimationSpec,
        )
    }
}

@Composable
fun NumberPicker(
    value: Int,
    range: Iterable<Int>,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: PickerColors = PickerDefaults.colors(),
    labelStyle: TextStyle = PickerDefaults.labelStyle(),
    labelSize: DpSize = PickerDefaults.labelSize,
    dividerHeight: Dp = PickerDefaults.dividerHeight,
) {
    Picker(
        value = value,
        values = range.toList(),
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        labelStyle = labelStyle,
        labelSize = labelSize,
        dividerHeight = dividerHeight,
    )
}

@Composable
fun <T : Any> Picker(
    value: T,
    values: List<T>,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: PickerColors = PickerDefaults.colors(),
    labelStyle: TextStyle = PickerDefaults.labelStyle(),
    labelSize: DpSize = PickerDefaults.labelSize,
    dividerHeight: Dp = PickerDefaults.dividerHeight,
) {
    Picker(
        state = rememberPickerState(value, values, onValueChange),
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        labelSize = labelSize,
        dividerHeight = dividerHeight,
    ) { v, _ ->
        Text(
            text = v.toString(),
            style = labelStyle,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T : Any> Picker(
    state: PickerState<T>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: PickerColors = PickerDefaults.colors(),
    labelSize: DpSize = PickerDefaults.labelSize,
    dividerHeight: Dp = PickerDefaults.dividerHeight,
    @FloatRange(from = 0.0, to = 1.0)
    labelMinAlpha: Float = 0.3f,
    flingBehavior: TargetedFlingBehavior = PickerDefaults.flingBehavior(state),
    label: @Composable (value: T, enabled: Boolean) -> Unit = { v, _ ->
        Text(v.toString())
    },
) {
    val labelProvider = rememberPickerLabelProvider(
        values = state.values,
        label = label,
        enabled = enabled,
        contentColor = colors.contentColor(enabled),
    )

    val mediator = remember {
        PickerDragMediator()
    }.also {
        it.state = state
        it.flingBehavior = flingBehavior
        it.scope = rememberCoroutineScope()
    }

    val gestureModifier = if (enabled) {
        Modifier.pointerInput(mediator) {
            detectVerticalDragGestures(
                onDragStart = mediator::onDragStart,
                onVerticalDrag = mediator::onDrag,
                onDragEnd = mediator::onDragEnd,
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(
                width = labelSize.width,
                height = labelSize.height * 3 + dividerHeight * 2,
            )
            .then(gestureModifier)
    ) {
        PickerLabels(
            state = state,
            labelProvider = labelProvider,
            labelSize = labelSize,
            labelMinAlpha = labelMinAlpha,
            dividerHeight = dividerHeight,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dividerHeight)
                    .background(colors.dividerColor(enabled))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dividerHeight)
                    .background(colors.dividerColor(enabled))
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> PickerLabels(
    state: PickerState<T>,
    labelProvider: () -> PickerLabelProvider<T>,
    labelSize: DpSize,
    labelMinAlpha: Float,
    dividerHeight: Dp,
    modifier: Modifier = Modifier,
) {
    LazyLayout(
        itemProvider = labelProvider,
        modifier = modifier.clipToBounds(),
    ) { constraints ->
        val width = labelSize.width.roundToPx().coerceIn(
            minimumValue = constraints.minWidth,
            maximumValue = constraints.maxWidth,
        )
        val height = (labelSize.height * 3 + dividerHeight * 2).roundToPx().coerceIn(
            minimumValue = constraints.minHeight,
            maximumValue = constraints.maxHeight,
        )
        val labelHeight = (height - dividerHeight.toPx() * 2) / 3f

        val labelConstraints = Constraints.fixed(
            width = width,
            height = floor(labelHeight).roundToInt(),
        )
        val indices = state.getVisibleLabelIndices()
        val placeableMap = indices.associateWith { index ->
            measure(index, labelConstraints).first()
        }

        val intervalHeight = floor(labelHeight + dividerHeight.toPx()).roundToInt()
        state.intervalHeight = intervalHeight

        layout(width, height) {
            placeableMap.forEach { (index, placeable) ->
                placeable.placeWithLayer(
                    x = 0,
                    y = ((state.offset + index) * intervalHeight).roundToInt(),
                ) {
                    val distance = (index - state.currentIndex).absoluteValue.coerceAtMost(1f)
                    alpha = 1f - (1f - labelMinAlpha) * distance
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun <T> rememberPickerLabelProvider(
    values: List<T>,
    label: @Composable (T, Boolean) -> Unit,
    enabled: Boolean,
    contentColor: Color,
): () -> PickerLabelProvider<T> {
    val provider = remember {
        PickerLabelProvider<T>()
    }.also {
        it.values = values
        it.label = label
        it.enabled = enabled
        it.contentColor = contentColor
    }
    return remember {
        { provider }
    }
}

@ExperimentalFoundationApi
internal class PickerLabelProvider<T> : LazyLayoutItemProvider {
    internal var values: List<T> by mutableStateOf(emptyList())
    internal var label: (@Composable (T, Boolean) -> Unit) by mutableStateOf({ _, _ -> })
    internal var enabled: Boolean by mutableStateOf(false)
    internal var contentColor: Color by mutableStateOf(Color.Unspecified)

    override val itemCount by derivedStateOf { values.size }

    @Composable
    override fun Item(index: Int, key: Any) {
        val value = values[index]
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides contentColor
            ) {
                label(value, enabled)
            }
        }
    }
}

@Composable
fun <T> rememberPickerState(
    value: T,
    values: List<T>,
    onValueChange: (T) -> Unit,
): PickerState<T> {
    val index = values.indexOf(value)
    val state = rememberPickerState(values, index)

    // value changed outside the picker
    LaunchedEffect(state, index) {
        if (state.settleIndex != index) {
            state.scrollToIndex(index)
        }
    }

    // invoke callback
    val latestValue by rememberUpdatedState(value)
    val latestCallback by rememberUpdatedState(onValueChange)
    LaunchedEffect(state) {
        snapshotFlow { state.settleIndex }
            .drop(1)
            .map { state.values[it] }
            .filter { it != latestValue }
            .collect {
                latestCallback(it)
            }
    }

    return state
}

@Composable
fun <T> rememberPickerState(
    values: List<T>,
    initialIndex: Int,
): PickerState<T> = remember(values) {
    PickerState(
        values = values,
        initialIndex = initialIndex,
    )
}

@Stable
class PickerState<out T> internal constructor(
    val values: List<T>,
    initialIndex: Int,
) {
    internal var index by mutableFloatStateOf(initialIndex.toFloat())
    internal var target by mutableIntStateOf(initialIndex)
    internal var intervalHeight: Int = 0

    /**
     * Normalized offset of each displayed label.
     *
     * Note: offset of the currently selected value is 1,
     * because additional 2 labels on both sides (top and bottom) will be displayed.
     *
     * - `offset == 1` when the first item is currently selected
     * - `offset == 1-n` when the n-th (0 <= n < values.size) item is currently selected
     */
    val offset: Float
        get() = 1f - index

    /**
     * An index of currently selected value.
     *
     * This index may be non-integer while scrolling or snap (fling) animation running.
     */
    val currentIndex: Float
        get() = index

    /**
     * An index of value to which the current picker should be snapped.
     *
     * This index must be the same value of [currentIndex] when no scroll or snap (fling) animation is running.
     *
     * This snap position only takes account of the scroll offset, not the current scroll (fling) velocity.
     */
    val snapIndex: Int by derivedStateOf { index.roundToInt() }

    /**
     * An index of value to which the picker should be snapped.
     *
     * Unlike [snapIndex], this index can only be updated when a user scrolling is completed
     * and the final snap position is determined.
     */
    val targetIndex: Int
        get() = target

    private var currentSettleIndex = initialIndex

    /**
     * An index of currently selected value.
     *
     * Unlike [currentIndex], this index is NOT changed while user scrolling or snap (fling) animation running.
     */
    val settleIndex: Int by derivedStateOf {
        val current = this.index
        val target = this.target
        if ((target - current).absoluteValue < 1e-6) {
            currentSettleIndex = target
            target
        } else {
            currentSettleIndex
        }
    }

    fun scrollToIndex(index: Int) {
        val target = index.coerceIn(0, values.size - 1)
        this.index = target.toFloat()
        this.target = target
    }

    internal fun getVisibleLabelIndices(): Iterable<Int> {
        val lower = floor(index - 1).roundToInt()
        val upper = ceil(index + 1).roundToInt()
        return max(lower, 0)..min(upper, values.size - 1)
    }
}

internal class PickerDragMediator : ScrollScope {
    private val velocityTracker = VelocityTracker()

    lateinit var state: PickerState<Any>
    lateinit var flingBehavior: TargetedFlingBehavior
    lateinit var scope: CoroutineScope

    fun onDragStart(point: Offset) {
        velocityTracker.resetTracking()
    }

    fun onDrag(change: PointerInputChange, amount: Float) {
        velocityTracker.addPosition(
            timeMillis = change.uptimeMillis,
            position = change.position,
        )
        // no dispatch to NestedScroll chains
        scrollBy(amount)
    }

    fun onDragEnd() {
        val velocity = velocityTracker.calculateVelocity()
        velocityTracker.resetTracking()

        scope.launch {
            with(flingBehavior) {
                // no dispatch to NestedScroll chains
                performFling(velocity.y)
            }
        }
    }

    override fun scrollBy(pixels: Float): Float {
        val interval = state.intervalHeight
        return if (interval > 0) {
            val currentIndex = state.index
            val targetIndex = (currentIndex - pixels / interval).coerceIn(0f, state.values.size - 1f)
            state.index = targetIndex
            -(targetIndex - currentIndex) * interval
        } else {
            0f
        }
    }
}

internal class PickerSnapLayoutProvider(
    private val state: PickerState<Any>,
    private val flingEnabled: Boolean,
) : SnapLayoutInfoProvider {
    override fun calculateSnapOffset(velocity: Float): Float {
        // ignore sign of velocity
        val snapIndex = state.snapIndex
        val interval = state.intervalHeight
        return if (interval > 0) {
            state.target = snapIndex
            -(snapIndex - state.currentIndex) * interval
        } else {
            0f
        }
    }

    override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float {
        if (!flingEnabled) {
            return calculateSnapOffset(velocity)
        }
        val interval = state.intervalHeight
        return if (interval > 0) {
            val currentIndex = state.currentIndex
            val decayIndex = currentIndex - decayOffset / interval
            val snapIndex = decayIndex.roundToInt().coerceIn(0, state.values.size - 1)
            state.target = snapIndex
            -(snapIndex - currentIndex) * interval
        } else {
            0f
        }
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
                NumberPicker(
                    value = value,
                    onValueChange = { value = it },
                    range = 1..100,
                    enabled = true,
                    modifier = Modifier
                        .padding(10.dp)
                        .width(80.dp),
                )
                NumberPicker(
                    value = value,
                    onValueChange = { value = it },
                    range = 1..100,
                    enabled = false,
                    modifier = Modifier
                        .padding(10.dp)
                        .width(80.dp),
                )
            }
        }
    }
}

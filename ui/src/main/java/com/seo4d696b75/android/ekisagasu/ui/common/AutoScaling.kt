package com.seo4d696b75.android.ekisagasu.ui.common

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun AutoScaling(
    modifier: Modifier = Modifier,
    @FloatRange(from = 0.5, to = 1.0)
    minScaleX: Float = 0.5f,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = content,
    ) { measurables, constraints ->
        require(measurables.size == 1) {
            "content size must be 1"
        }
        val placable = measurables.first().measure(
            constraints.copy(
                maxWidth = floor(constraints.maxWidth / minScaleX).roundToInt(),
            ),
        )
        val scale = if (placable.width <= constraints.maxWidth) {
            1f
        } else {
            max(constraints.maxWidth.toFloat() / placable.width, minScaleX)
        }
        layout(
            width = min(placable.width, constraints.maxWidth),
            height = placable.height,
        ) {
            placable.placeRelativeWithLayer(0, 0) {
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = scale
            }
        }
    }
}

@Composable
fun AutoScalingText(
    text: String,
    modifier: Modifier = Modifier,
    @FloatRange(from = 0.5, to = 1.0)
    minScaleX: Float = 0.5f,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    softWrap: Boolean = true,
    maxLines: Int = 1,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current
) {
    AutoScaling(
        modifier = modifier,
        minScaleX = minScaleX,
    ) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            onTextLayout = onTextLayout,
            style = style,
        )
    }
}

@Composable
fun AutoScalingText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    @FloatRange(from = 0.5, to = 1.0)
    minScaleX: Float = 0.5f,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    softWrap: Boolean = true,
    maxLines: Int = 1,
    minLines: Int = 1,
    style: TextStyle = LocalTextStyle.current
) {
    AutoScaling(
        modifier = modifier,
        minScaleX = minScaleX,
    ) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            style = style,
        )
    }
}

@Composable
@Preview
private fun AutoScalingTextPreview() {
    AppTheme {
        Surface {
            Column(
                modifier = Modifier
                    .width(200.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AutoScalingText(text = "短いテキスト")
                AutoScalingText(text = "長いテキスト長いテキスト長いテキスト")
                AutoScalingText(text = "めっちゃ長いテキストめっちゃ長いテキストめっちゃ長いテキスト")
            }
        }
    }
}

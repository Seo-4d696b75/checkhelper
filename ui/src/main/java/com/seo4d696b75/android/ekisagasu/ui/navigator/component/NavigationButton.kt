package com.seo4d696b75.android.ekisagasu.ui.navigator.component

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme

@Composable
fun NavigationButton(
    onClick: () -> Unit,
    @DrawableRes id: Int,
    contentDescription: String,
    enabled: Boolean = true,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides 40.dp
    ) {
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.size(40.dp),
        ) {
            Icon(
                painter = painterResource(id = id),
                contentDescription = contentDescription,
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxSize(),
            )
        }
    }
}

@Composable
@PreviewLightDark
private fun NavigationButtonPreview() {
    AppTheme {
        Surface {
            NavigationButton(
                onClick = {},
                id = R.drawable.ic_line_selects,
                contentDescription = "",
            )
        }
    }
}

@Composable
@PreviewLightDark
private fun NavigationButtonPreview_disabled() {
    AppTheme {
        Surface {
            NavigationButton(
                onClick = {},
                id = R.drawable.ic_line_selects,
                contentDescription = "",
                enabled = false,
            )
        }
    }
}

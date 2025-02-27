package com.seo4d696b75.android.ekisagasu.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme

@Composable
fun OverlayAppIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(6.dp),
            )
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = "expand navigator section"
            },
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_app),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            contentDescription = null,
            modifier = Modifier.size(36.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun NavigatorShrinkIconPreview() {
    AppTheme {
        OverlayAppIcon(
            onClick = {},
            modifier = Modifier.size(53.dp),
        )
    }
}

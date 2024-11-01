package com.seo4d696b75.android.ekisagasu.ui.navigator.component

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme

@Composable
fun NavigatorShrinkIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(width = 52.dp, height = 53.dp)
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
            painter = painterResource(id = R.drawable.launch_icon),
            contentDescription = null,
            modifier = Modifier.size(36.dp),
        )
    }
}

@Preview
@Composable
private fun NavigatorShrinkIconPreview() {
    AppTheme {
        NavigatorShrinkIcon(onClick = {})
    }
}

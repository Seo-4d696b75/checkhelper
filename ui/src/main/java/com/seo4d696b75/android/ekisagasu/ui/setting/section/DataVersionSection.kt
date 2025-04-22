package com.seo4d696b75.android.ekisagasu.ui.setting.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataVersion
import com.seo4d696b75.android.ekisagasu.domain.date.TIME_PATTERN_DATETIME
import com.seo4d696b75.android.ekisagasu.domain.date.format
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.setting.DataVersionUiState
import com.seo4d696b75.android.ekisagasu.ui.theme.AppTheme
import java.util.Date

@Composable
fun DataVersionSection(
    state: DataVersionUiState,
    checkLatestData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = stringResource(id = R.string.setting_title_data),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when (state) {
                    DataVersionUiState.Undefined -> ""

                    is DataVersionUiState.Defined -> stringResource(
                        id = R.string.text_data_version,
                        state.version.version,
                    )
                },
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (state) {
                    DataVersionUiState.Undefined -> ""

                    is DataVersionUiState.Defined -> stringResource(
                        id = R.string.text_data_updated_at,
                        state.version.timestamp.format(TIME_PATTERN_DATETIME),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (state is DataVersionUiState.Defined) {
            Button(
                onClick = checkLatestData,
                modifier = Modifier
                    .widthIn(min = 160.dp)
                    .padding(12.dp)
            ) {
                when (state) {
                    is DataVersionUiState.Idle -> {
                        Text(
                            text = stringResource(id = R.string.setting_mes_check_latest_data),
                        )

                    }

                    is DataVersionUiState.Checking -> {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.setting_mes_checking_latest_data),
                        )
                    }

                    is DataVersionUiState.LatestChecked -> {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.setting_mes_checked_latest_data),
                        )
                    }
                }
            }
        }
    }
}

private class DataVersionSectionPreviewParamProvider : PreviewParameterProvider<DataVersionUiState> {
    val data = DataVersion(
        version = 20241220L,
        timestamp = Date(),
    )

    override val values = sequenceOf(
        DataVersionUiState.Undefined,
        DataVersionUiState.Idle(data),
        DataVersionUiState.Checking(data),
        DataVersionUiState.LatestChecked(data),
    )
}

@Composable
@PreviewLightDark
private fun DataVersionSectionPreview(
    @PreviewParameter(DataVersionSectionPreviewParamProvider::class)
    state: DataVersionUiState,
) {
    AppTheme {
        Surface {
            DataVersionSection(
                state = state,
                checkLatestData = {},
            )
        }
    }
}

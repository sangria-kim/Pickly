package com.cola.pickly.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class PicklySnackbarType {
    Info,
    Success,
    Error
}

@Composable
fun PicklySnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    typeResolver: (SnackbarData) -> PicklySnackbarType = { PicklySnackbarType.Info }
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { snackbarData ->
        val type = typeResolver(snackbarData)
        val actionLabel = snackbarData.visuals.actionLabel

        val containerColor = when (type) {
            PicklySnackbarType.Info -> MaterialTheme.colorScheme.inverseSurface
            PicklySnackbarType.Success -> MaterialTheme.colorScheme.inverseSurface
            PicklySnackbarType.Error -> MaterialTheme.colorScheme.inverseSurface
        }
        val contentColor = MaterialTheme.colorScheme.inverseOnSurface

        Snackbar(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            containerColor = containerColor,
            contentColor = contentColor,
            action = if (actionLabel != null) {
                {
                    TextButton(onClick = { snackbarData.performAction() }) {
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }
                }
            } else {
                null
            }
        ) {
            Text(
                text = snackbarData.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


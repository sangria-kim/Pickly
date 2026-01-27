package com.cola.pickly.feature.organize.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cola.pickly.core.ui.R

sealed class BulkAction(
    @StringRes val labelResId: Int,
    val icon: ImageVector
) {
    object Share : BulkAction(R.string.bulk_action_share, Icons.Default.Share)
    object Move : BulkAction(R.string.bulk_action_move, Icons.Default.DriveFileMove)
    object Copy : BulkAction(R.string.bulk_action_copy, Icons.Default.ContentCopy)
    object Delete : BulkAction(R.string.bulk_action_delete, Icons.Default.Delete)

    companion object {
        val actions = listOf(Share, Move, Copy, Delete)
    }
}

/**
 * Bulk Action Bar Composable
 * Multi Select Mode에서 Bottom Area에 표시되는 일괄 액션 바
 */
@Composable
fun BulkActionBar(
    onShareClick: () -> Unit,
    onMoveClick: () -> Unit,
    onCopyClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isActionInProgress: Boolean = false
) {
    NavigationBar(
        modifier = Modifier.height(104.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.outline
    ) {
        BulkAction.actions.forEach { action ->
            val onClickHandler = when (action) {
                is BulkAction.Share -> onShareClick
                is BulkAction.Move -> onMoveClick
                is BulkAction.Copy -> onCopyClick
                is BulkAction.Delete -> onDeleteClick
            }
            
            NavigationBarItem(
                icon = { Icon(action.icon, contentDescription = stringResource(action.labelResId)) },
                label = { Text(stringResource(action.labelResId)) },
                selected = false,
                onClick = if (isActionInProgress) { {} } else onClickHandler,
                enabled = !isActionInProgress,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.outline,
                    unselectedTextColor = MaterialTheme.colorScheme.outline,
                    indicatorColor = Color.Transparent,
                    disabledIconColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
                    disabledTextColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
                )
            )
        }
    }
}


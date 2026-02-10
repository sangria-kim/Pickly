package com.cola.pickly.feature.organize.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Share
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
    object Share : BulkAction(R.string.bulk_action_share, Icons.Outlined.Share)
    object Move : BulkAction(R.string.bulk_action_move, Icons.Outlined.DriveFileMove)
    object Copy : BulkAction(R.string.bulk_action_copy, Icons.Outlined.ContentCopy)
    object Delete : BulkAction(R.string.bulk_action_delete, Icons.Outlined.Delete)

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

            val itemColor = when (action) {
                is BulkAction.Delete -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.outline
            }
            val disabledColor = itemColor.copy(alpha = 0.38f)

            NavigationBarItem(
                icon = { Icon(action.icon, contentDescription = stringResource(action.labelResId)) },
                label = { Text(stringResource(action.labelResId)) },
                selected = false,
                onClick = if (isActionInProgress) { {} } else onClickHandler,
                enabled = !isActionInProgress,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = itemColor,
                    selectedTextColor = itemColor,
                    unselectedIconColor = itemColor,
                    unselectedTextColor = itemColor,
                    indicatorColor = Color.Transparent,
                    disabledIconColor = disabledColor,
                    disabledTextColor = disabledColor
                )
            )
        }
    }
}


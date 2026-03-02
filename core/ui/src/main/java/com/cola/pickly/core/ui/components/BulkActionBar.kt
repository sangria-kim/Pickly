package com.cola.pickly.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.RemoveCircle
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.cola.pickly.core.ui.R
import com.cola.pickly.core.ui.theme.RejectRed
import com.cola.pickly.core.ui.theme.TealAccent

sealed class BulkAction(
    @StringRes val labelResId: Int,
    val icon: ImageVector
) {
    object Accept : BulkAction(R.string.bulk_action_accept, Icons.Outlined.CheckCircle)
    object Unmark : BulkAction(R.string.bulk_action_unmark, Icons.Outlined.RemoveCircle)
    object Reject : BulkAction(R.string.bulk_action_reject, Icons.Outlined.Cancel)
    object Share : BulkAction(R.string.bulk_action_share, Icons.Outlined.Share)
    object Move : BulkAction(R.string.bulk_action_move, Icons.Outlined.DriveFileMove)
    object Copy : BulkAction(R.string.bulk_action_copy, Icons.Outlined.ContentCopy)
    object Delete : BulkAction(R.string.bulk_action_delete, Icons.Outlined.Delete)

    companion object {
        val organizeActions = listOf(Accept, Unmark, Reject, Delete)
        val archiveActions = listOf(Share, Move, Copy, Delete)
    }
}

@Composable
fun BulkActionBar(
    actions: List<BulkAction>,
    onActionClick: (BulkAction) -> Unit,
    isActionInProgress: Boolean = false
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.outline
    ) {
        // 정리하기 탭 멀티 셀렉트 전용 배치:
        // 채택(25%) · 해제(25%) · 제외(25%) · 삭제(25%)
        if (actions == BulkAction.organizeActions) {
            val organizeSlots: List<BulkAction> = listOf(
                BulkAction.Accept,
                BulkAction.Unmark,
                BulkAction.Reject,
                BulkAction.Delete
            )

            organizeSlots.forEach { action ->
                val itemColor = when (action) {
                    is BulkAction.Accept -> TealAccent
                    is BulkAction.Reject -> RejectRed
                    is BulkAction.Delete -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.outline
                }
                val disabledColor = itemColor.copy(alpha = 0.38f)

                NavigationBarItem(
                    icon = { Icon(action.icon, contentDescription = stringResource(action.labelResId)) },
                    label = { Text(stringResource(action.labelResId)) },
                    selected = false,
                    onClick = if (isActionInProgress) {
                        {}
                    } else {
                        { onActionClick(action) }
                    },
                    enabled = !isActionInProgress,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = itemColor,
                        selectedTextColor = itemColor,
                        unselectedIconColor = itemColor,
                        unselectedTextColor = itemColor,
                        indicatorColor = TealAccent.copy(alpha = 0.12f),
                        disabledIconColor = disabledColor,
                        disabledTextColor = disabledColor
                    )
                )
            }
        } else {
            actions.forEach { action ->
                val itemColor = when (action) {
                    is BulkAction.Delete -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.outline
                }
                val disabledColor = itemColor.copy(alpha = 0.38f)

                NavigationBarItem(
                    icon = { Icon(action.icon, contentDescription = stringResource(action.labelResId)) },
                    label = { Text(stringResource(action.labelResId)) },
                    selected = false,
                    onClick = if (isActionInProgress) {
                        {}
                    } else {
                        { onActionClick(action) }
                    },
                    enabled = !isActionInProgress,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = itemColor,
                        selectedTextColor = itemColor,
                        unselectedIconColor = itemColor,
                        unselectedTextColor = itemColor,
                        indicatorColor = TealAccent.copy(alpha = 0.12f),
                        disabledIconColor = disabledColor,
                        disabledTextColor = disabledColor
                    )
                )
            }
        }
    }
}

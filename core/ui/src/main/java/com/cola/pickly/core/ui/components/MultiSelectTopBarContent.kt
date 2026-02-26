package com.cola.pickly.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cola.pickly.core.ui.R

@Composable
fun MultiSelectTopBarContent(
    selectedCount: Int,
    onCancelClick: () -> Unit,
    contentColor: Color = Color.White,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onCancelClick,
            modifier = Modifier.padding(start = 0.dp, end = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "취소",
                tint = contentColor
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.selected_count_format, selectedCount),
            style = MaterialTheme.typography.titleMedium,
            color = contentColor
        )
        actions()
    }
}

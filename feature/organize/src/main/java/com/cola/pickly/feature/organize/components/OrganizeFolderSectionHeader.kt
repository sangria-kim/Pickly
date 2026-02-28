package com.cola.pickly.feature.organize.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.IndeterminateCheckBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.cola.pickly.core.ui.R
import com.cola.pickly.core.ui.theme.TealAccent
import com.cola.pickly.core.ui.theme.TextPrimary

@Composable
fun OrganizeFolderSectionHeader(
    folderName: String,
    onFolderSelectClick: () -> Unit,
    isMultiSelectMode: Boolean = false,
    selectAllState: ToggleableState = ToggleableState.Off,
    onSelectAllToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!isMultiSelectMode) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onFolderSelectClick
                    )
                } else Modifier
            )
            // 멀티셀렉트: start=5dp → 사진 체크박스와 동일 위치(grid 4dp + 5dp = 9dp)
            .padding(
                start = if (isMultiSelectMode) 5.dp else 12.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMultiSelectMode) {
            // 너비=20dp(아이콘), 높이=24dp(ArrowDropDown과 동일)로 Row 높이 고정
            Box(
                modifier = Modifier
                    .size(width = 20.dp, height = 24.dp)
                    .clickable(onClick = onSelectAllToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (selectAllState) {
                        ToggleableState.On -> Icons.Default.CheckBox
                        ToggleableState.Indeterminate -> Icons.Default.IndeterminateCheckBox
                        ToggleableState.Off -> Icons.Default.CheckBoxOutlineBlank
                    },
                    contentDescription = null,
                    tint = when (selectAllState) {
                        ToggleableState.On, ToggleableState.Indeterminate -> TealAccent
                        ToggleableState.Off -> MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = folderName.ifBlank { stringResource(R.string.folder_selection) },
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        if (!isMultiSelectMode) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = stringResource(R.string.folder_selection),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

package com.cola.pickly.feature.archive.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.IndeterminateCheckBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cola.pickly.core.ui.R
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.ui.theme.TealAccent
import com.cola.pickly.core.ui.theme.TextPrimary
import kotlin.math.floor

@Composable
fun ArchiveSection(
    folderName: String,
    photos: List<Photo>,
    onPhotoClick: (Photo) -> Unit,
    modifier: Modifier = Modifier,
    selectedIds: Set<Long> = emptySet(),
    isMultiSelectMode: Boolean = false,
    onToggleSelection: ((Long) -> Unit)? = null,
    onToggleFolderSelection: (() -> Unit)? = null
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val itemSize = 100.dp
    val spacing = 2.dp
    val horizontalPadding = 4.dp * 2
    val columns = floor((screenWidth - horizontalPadding + spacing) / (itemSize + spacing)).toInt().coerceAtLeast(3)
    val itemWidth = (screenWidth - horizontalPadding - spacing * (columns - 1)) / columns

    val photoIds = photos.map { it.id }.toSet()
    val folderSelectionState = when {
        photoIds.isEmpty() -> ToggleableState.Off
        selectedIds.containsAll(photoIds) -> ToggleableState.On
        photoIds.any { it in selectedIds } -> ToggleableState.Indeterminate
        else -> ToggleableState.Off
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 정리하기 탭 헤더(48dp)와 높이 맞춤: 12(top) + 24(ArrowDropDown) + 12(bottom) = 48dp
                .heightIn(min = 48.dp)
                // 멀티셀렉트: start=9dp → 사진 체크박스와 동일 위치(column 4dp + 5dp = 9dp)
                .padding(start = if (isMultiSelectMode) 9.dp else 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isMultiSelectMode) {
                    Icon(
                        imageVector = when (folderSelectionState) {
                            ToggleableState.On -> Icons.Default.CheckBox
                            ToggleableState.Indeterminate -> Icons.Default.IndeterminateCheckBox
                            ToggleableState.Off -> Icons.Default.CheckBoxOutlineBlank
                        },
                        contentDescription = when (folderSelectionState) {
                            ToggleableState.On -> "폴더 전체 선택 해제"
                            else -> "폴더 전체 선택"
                        },
                        tint = when (folderSelectionState) {
                            ToggleableState.On, ToggleableState.Indeterminate -> TealAccent
                            ToggleableState.Off -> MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onToggleFolderSelection?.invoke() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = folderName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                    color = TextPrimary
                )
            }
            Text(
                text = stringResource(R.string.archive_section_picks_format, photos.size),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                color = TextPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            photos.chunked(columns).forEach { rowPhotos ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    rowPhotos.forEach { photo ->
                        ArchivePhotoItem(
                            photo = photo,
                            onClick = { onPhotoClick(photo) },
                            modifier = Modifier.width(itemWidth),
                            isSelected = selectedIds.contains(photo.id),
                            isMultiSelectMode = isMultiSelectMode,
                            onToggleSelection = { onToggleSelection?.invoke(photo.id) }
                        )
                    }
                    repeat(columns - rowPhotos.size) {
                        Spacer(modifier = Modifier.width(itemWidth))
                    }
                }
            }
        }
    }
}

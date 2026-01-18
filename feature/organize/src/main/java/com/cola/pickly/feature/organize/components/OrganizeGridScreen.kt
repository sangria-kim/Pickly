package com.cola.pickly.feature.organize.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.model.PhotoSelectionState

@Composable
fun OrganizeGridScreen(
    photos: List<Photo>,
    selectedIds: Set<Long>,
    selectionMap: Map<Long, PhotoSelectionState> = emptyMap(),
    isMultiSelectMode: Boolean = false,
    onPhotoClick: (Photo) -> Unit,
    onToggleSelection: (Long) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(
            items = photos,
            key = { it.id }
        ) { photo ->
            PhotoGridItem(
                photo = photo,
                isSelected = selectedIds.contains(photo.id),
                selectionState = selectionMap[photo.id],
                isMultiSelectMode = isMultiSelectMode,
                onClick = { onPhotoClick(photo) },
                onToggleSelection = { onToggleSelection(photo.id) }
            )
        }
    }
}

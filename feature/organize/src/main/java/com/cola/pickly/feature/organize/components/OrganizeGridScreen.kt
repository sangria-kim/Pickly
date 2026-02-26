package com.cola.pickly.feature.organize.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.model.RejectReason
@Composable
fun OrganizeGridScreen(
    photos: List<Photo>,
    selectedIds: Set<Long>,
    selectionMap: Map<Long, PhotoSelectionState> = emptyMap(),
    isMultiSelectMode: Boolean = false,
    isAnalyzing: Boolean = false,
    autoRejectCandidates: Map<Long, RejectReason> = emptyMap(),
    gridState: LazyGridState = rememberLazyGridState(),
    onPhotoClick: (Photo) -> Unit,
    onToggleSelection: (Long) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        modifier = Modifier.fillMaxSize(),
        state = gridState,
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
                isAnalyzing = isAnalyzing,
                autoRejectReason = autoRejectCandidates[photo.id],
                onClick = { onPhotoClick(photo) },
                onToggleSelection = { onToggleSelection(photo.id) }
            )
        }
    }
}

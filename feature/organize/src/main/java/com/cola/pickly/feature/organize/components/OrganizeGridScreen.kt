package com.cola.pickly.feature.organize.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.model.RejectReason
import com.cola.pickly.core.ui.components.BurstRecommendRank

@Composable
fun OrganizeGridScreen(
    selectedFolderName: String,
    photos: List<Photo>,
    selectedIds: Set<Long>,
    selectionMap: Map<Long, PhotoSelectionState> = emptyMap(),
    isMultiSelectMode: Boolean = false,
    isAnalyzing: Boolean = false,
    autoRejectCandidates: Map<Long, RejectReason> = emptyMap(),
    burstRecommendMap: Map<Long, BurstRecommendRank> = emptyMap(),
    burstDebugMap: Map<Long, BurstDebugInfo> = emptyMap(),
    gridState: LazyGridState = rememberLazyGridState(),
    selectAllState: ToggleableState = ToggleableState.Off,
    onFolderSelectClick: () -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onSelectAllToggle: () -> Unit = {}
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
            count = 1,
            key = { "folder_header" },
            span = { GridItemSpan(maxLineSpan) }
        ) {
            OrganizeFolderSectionHeader(
                folderName = selectedFolderName,
                onFolderSelectClick = onFolderSelectClick,
                isMultiSelectMode = isMultiSelectMode,
                selectAllState = selectAllState,
                onSelectAllToggle = onSelectAllToggle
            )
        }

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
                burstRecommendRank = burstRecommendMap[photo.id],
                burstDebugInfo = burstDebugMap[photo.id],
                onClick = { onPhotoClick(photo) },
                onToggleSelection = { onToggleSelection(photo.id) }
            )
        }
    }
}

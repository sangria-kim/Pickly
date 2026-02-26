package com.cola.pickly.feature.archive.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cola.pickly.core.ui.R
import com.cola.pickly.core.model.Photo
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
    onToggleSelection: ((Long) -> Unit)? = null
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val itemSize = 100.dp
    val spacing = 2.dp
    val horizontalPadding = 4.dp * 2
    val columns = floor((screenWidth - horizontalPadding + spacing) / (itemSize + spacing)).toInt().coerceAtLeast(3)
    val itemWidth = (screenWidth - horizontalPadding - spacing * (columns - 1)) / columns

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = folderName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                color = TextPrimary
            )
            Text(
                text = stringResource(R.string.archive_section_picks_format, photos.size),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                color = TextPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
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

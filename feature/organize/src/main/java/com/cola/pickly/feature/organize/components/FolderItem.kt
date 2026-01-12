package com.cola.pickly.feature.organize.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cola.pickly.core.ui.R
import com.cola.pickly.core.model.PhotoFolder

@Composable
fun FolderItem(
    folder: PhotoFolder,
    onClick: (PhotoFolder) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(folder) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(folder.thumbnailUri)
                .crossfade(true)
                .size(128) // 썸네일 크기 제한 (64.dp * 2 = 128px)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Info
        Column {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF121212) // #121212
            )
            Text(
                text = stringResource(R.string.photo_count_format, folder.count),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF121212) // #121212
            )
        }
    }
}

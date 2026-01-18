package com.cola.pickly.feature.archive.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cola.pickly.core.ui.R
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.ui.theme.TextPrimary
import kotlin.math.floor

/**
 * S-06 아카이브 화면의 폴더별 섹션 컴포넌트
 * 
 * Wireframe.md S-06 참고:
 * - 섹션 헤더: "폴더명 N Picks" 형식
 * - 그리드로 사진 표시 (3-4열 그리드)
 * - ArchivePhotoItem을 사용하여 사진 표시
 */
@Composable
fun ArchiveSection(
    folderName: String,
    photos: List<Photo>,
    onPhotoClick: (Photo) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val itemSize = 100.dp
    val spacing = 2.dp
    val horizontalPadding = 4.dp * 2 // 좌우 패딩 (정리하기 탭과 동일)
    val columns = floor((screenWidth - horizontalPadding + spacing) / (itemSize + spacing)).toInt().coerceAtLeast(3)
    val itemWidth = (screenWidth - horizontalPadding - spacing * (columns - 1)) / columns

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // 섹션 헤더: 폴더명 왼쪽 정렬, "n Picks" 오른쪽 정렬
        // Top bar 타이틀과 동일한 들여쓰기 (16.dp)
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

        // 사진 그리드 (정리하기 탭과 동일한 여백: 4.dp)
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
                            modifier = Modifier.width(itemWidth)
                        )
                    }
                    // 빈 공간 채우기 (마지막 행이 columns보다 적을 때)
                    repeat(columns - rowPhotos.size) {
                        Spacer(modifier = Modifier.width(itemWidth))
                    }
                }
            }
        }
    }
}


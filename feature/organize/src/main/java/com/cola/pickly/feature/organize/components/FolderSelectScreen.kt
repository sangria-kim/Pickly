package com.cola.pickly.feature.organize.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cola.pickly.core.model.PhotoFolder

enum class FolderSelectMode {
    FolderSelection,      // "폴더 선택"
    DestinationSelection  // "목적지 폴더 선택" + "만들기" 버튼
}

@Composable
fun FolderSelectScreen(
    folders: List<PhotoFolder>,
    isLoading: Boolean = false,
    mode: FolderSelectMode = FolderSelectMode.FolderSelection,
    onClose: () -> Unit,
    onFolderClick: (PhotoFolder) -> Unit,
    onCreateFolderClick: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth() // 좌우 패딩 제거
                    .heightIn(max = 500.dp), // 높이를 스크린샷 비율에 맞춰 제한
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), // 상단만 둥글게
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(20.dp) // 내부 패딩 확대
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (mode) {
                                FolderSelectMode.FolderSelection -> "폴더 선택"
                                FolderSelectMode.DestinationSelection -> "목적지 폴더 선택"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        if (mode == FolderSelectMode.DestinationSelection && onCreateFolderClick != null) {
                            TextButton(onClick = onCreateFolderClick) {
                                Text(
                                    text = "만들기",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // List
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(min = 200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (folders.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(min = 200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "표시할 폴더가 없습니다.", color = MaterialTheme.colorScheme.onSurface)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            items(folders) { folder ->
                                FolderItem(
                                    folder = folder,
                                    onClick = onFolderClick
                                )
                            }
                        }
                    }
                    
                    // Footer (Cancel Button)
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = onClose,
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(
                                text = "취소",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

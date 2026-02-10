package com.cola.pickly.feature.archive.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.ui.transition.ContainerTransformSpec
import com.cola.pickly.core.ui.transition.LocalAnimatedVisibilityScope
import com.cola.pickly.core.ui.transition.LocalSharedTransitionScope
import java.io.File

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ArchivePhotoItem(
    photo: Photo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onClickState = rememberUpdatedState(onClick)
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = { onClickState.value() })
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(File(photo.filePath))
                .crossfade(true)
                .size(512)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "photo_${photo.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = ContainerTransformSpec.PhotoContainerTransform
                            )
                        }
                    } else Modifier
                )
        )
    }
}

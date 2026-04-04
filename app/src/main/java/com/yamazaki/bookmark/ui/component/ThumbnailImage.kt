package com.yamazaki.bookmark.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage

@Composable
fun ThumbnailImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    if (url.isNullOrBlank()) {
        PlaceholderImage(modifier)
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            loading = {
                PlaceholderImage(Modifier.fillMaxSize())
            },
            error = {
                PlaceholderImage(Modifier.fillMaxSize())
            }
        )
    }
}

@Composable
private fun PlaceholderImage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Bookmark,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

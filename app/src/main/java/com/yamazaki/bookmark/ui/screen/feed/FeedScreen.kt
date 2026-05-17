package com.yamazaki.bookmark.ui.screen.feed

import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yamazaki.bookmark.domain.model.Bookmark
import com.yamazaki.bookmark.ui.component.ThumbnailImage

@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onNavigateBack: () -> Unit
) {
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    // Must be called unconditionally (Rules of Compose)
    val pagerState = rememberPagerState(pageCount = { bookmarks.size })

    if (bookmarks.isEmpty()) {
        FeedEmptyState(onNavigateBack = onNavigateBack)
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            // Guard against list shrinking while pager holds a stale index
            val bookmark = bookmarks.getOrNull(page) ?: return@VerticalPager
            FeedPage(
                bookmark = bookmark,
                isActive = page == pagerState.settledPage
            )
        }

        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .systemBarsPadding()
                .padding(8.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "戻る",
                tint = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.45f), shape = CircleShape)
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun FeedPage(bookmark: Bookmark, isActive: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        ThumbnailImage(
            url = bookmark.thumbnailUrl,
            contentDescription = bookmark.title,
            modifier = Modifier.fillMaxSize()
        )
        if (isActive) {
            FeedWebView(bookmark = bookmark)
        }
        FeedPageInfo(
            title = bookmark.title,
            url = bookmark.url,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun FeedWebView(bookmark: Bookmark) {
    val embedUrl = remember(bookmark.thumbnailUrl, bookmark.url) { resolveEmbedUrl(bookmark) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var webViewRef: WebView? by remember { mutableStateOf(null) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    @Suppress("DEPRECATION")
                    mediaPlaybackRequiresUserGesture = false
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                loadUrl(embedUrl)
                webViewRef = this
            }
        }
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webViewRef?.onPause()
                Lifecycle.Event.ON_RESUME -> webViewRef?.onResume()
                Lifecycle.Event.ON_DESTROY -> { webViewRef?.destroy(); webViewRef = null }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewRef?.apply { stopLoading(); loadUrl("about:blank"); onPause(); destroy() }
            webViewRef = null
        }
    }
}

@Composable
private fun FeedPageInfo(title: String, url: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f))
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = url,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FeedEmptyState(onNavigateBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "サムネイル付きのブックマークがありません",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onNavigateBack) { Text("戻る") }
        }
    }
}

private fun resolveEmbedUrl(bookmark: Bookmark): String {
    val thumbnailUrl = bookmark.thumbnailUrl ?: return bookmark.url
    // thumbnailUrl for YouTube: https://img.youtube.com/vi/{ID}/mqdefault.jpg
    val videoId = Regex("""img\.youtube\.com/vi/([a-zA-Z0-9_-]{11})/""")
        .find(thumbnailUrl)?.groupValues?.get(1)
    return if (videoId != null) "https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1"
    else bookmark.url
}

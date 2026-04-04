package com.yamazaki.bookmark

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yamazaki.bookmark.ui.navigation.AddRoute
import com.yamazaki.bookmark.ui.navigation.DetailRoute
import com.yamazaki.bookmark.ui.navigation.ListRoute
import com.yamazaki.bookmark.ui.screen.add.AddScreen
import com.yamazaki.bookmark.ui.screen.add.AddViewModel
import com.yamazaki.bookmark.ui.screen.detail.DetailScreen
import com.yamazaki.bookmark.ui.screen.detail.DetailViewModel
import com.yamazaki.bookmark.ui.screen.list.ListScreen
import com.yamazaki.bookmark.ui.screen.list.ListViewModel
import com.yamazaki.bookmark.ui.theme.BookMarkTheme

class MainActivity : ComponentActivity() {

    private var pendingShareUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleShareIntent(intent)

        setContent {
            BookMarkTheme {
                val navController = rememberNavController()
                val app = application as BookMarkApplication

                // Navigate to Add screen when share URL arrives
                // Using LaunchedEffect to avoid composition side-effects
                val shareUrl = pendingShareUrl
                LaunchedEffect(shareUrl) {
                    if (shareUrl != null) {
                        pendingShareUrl = null
                        navController.navigate(AddRoute(prefillUrl = shareUrl)) {
                            launchSingleTop = true
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ListRoute
                ) {
                    composable<ListRoute> {
                        val vm: ListViewModel = viewModel(
                            factory = ListViewModel.Factory(app.repository)
                        )
                        ListScreen(
                            viewModel = vm,
                            onNavigateToAdd = {
                                navController.navigate(AddRoute())
                            },
                            onNavigateToDetail = { id ->
                                navController.navigate(DetailRoute(bookmarkId = id))
                            }
                        )
                    }
                    composable<AddRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<AddRoute>()
                        val vm: AddViewModel = viewModel(
                            factory = AddViewModel.Factory(
                                app.repository,
                                app.ogpFetcher,
                                route.prefillUrl
                            )
                        )
                        AddScreen(
                            viewModel = vm,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable<DetailRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<DetailRoute>()
                        val vm: DetailViewModel = viewModel(
                            factory = DetailViewModel.Factory(
                                app.repository,
                                route.bookmarkId
                            )
                        )
                        DetailScreen(
                            viewModel = vm,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the stored intent
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_SEND -> {
                // Handle text/plain shares (Chrome, other browsers)
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT)

                val url = extractUrl(sharedText) ?: extractUrl(sharedSubject)
                if (url != null) {
                    pendingShareUrl = url
                    // Clear the intent action to prevent re-processing
                    intent.action = null
                } else if (sharedText != null || sharedSubject != null) {
                    // Had text but no URL found
                    Toast.makeText(this, "URLが見つかりませんでした", Toast.LENGTH_SHORT).show()
                }
            }
            Intent.ACTION_VIEW -> {
                // Handle direct URL opens (e.g., from other apps)
                val url = intent.data?.toString()
                if (url != null && url.startsWith("http")) {
                    pendingShareUrl = url
                    intent.action = null
                }
            }
        }
    }

    private fun extractUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null

        // Try to find a URL in the text
        // Chrome typically sends just the URL, or "Title - URL", or "Title\nURL"
        val urlPattern = Regex("""https?://[^\s<>"{}|\\^`\[\]]+""")
        val match = urlPattern.find(text)?.value ?: return null

        // Clean trailing punctuation that's likely not part of the URL
        return match.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '>')
    }
}

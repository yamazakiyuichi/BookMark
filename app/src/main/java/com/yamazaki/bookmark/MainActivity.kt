package com.yamazaki.bookmark

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

                // Handle pending share URL
                pendingShareUrl?.let { url ->
                    pendingShareUrl = null
                    navController.navigate(AddRoute(prefillUrl = url))
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
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            val url = extractUrl(sharedText)
            if (url != null) {
                pendingShareUrl = url
            }
        }
    }

    private fun extractUrl(text: String): String? {
        val urlPattern = Regex("""https?://\S+""")
        return urlPattern.find(text)?.value
    }
}

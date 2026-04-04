package com.yamazaki.bookmark

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yamazaki.bookmark.data.local.BookmarkDatabase
import com.yamazaki.bookmark.data.remote.OgpFetcher
import com.yamazaki.bookmark.data.repository.BookmarkRepository
import com.yamazaki.bookmark.worker.LinkCheckWorker
import java.util.concurrent.TimeUnit

class BookMarkApplication : Application() {

    val database: BookmarkDatabase by lazy {
        BookmarkDatabase.getInstance(this)
    }

    val ogpFetcher: OgpFetcher by lazy { OgpFetcher() }

    val repository: BookmarkRepository by lazy {
        BookmarkRepository(database.bookmarkDao(), ogpFetcher)
    }

    override fun onCreate() {
        super.onCreate()
        scheduleLinkCheck()
    }

    private fun scheduleLinkCheck() {
        val request = PeriodicWorkRequestBuilder<LinkCheckWorker>(6, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "link_check",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

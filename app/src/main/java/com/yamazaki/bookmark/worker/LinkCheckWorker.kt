package com.yamazaki.bookmark.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yamazaki.bookmark.BookMarkApplication

class LinkCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as BookMarkApplication
        return try {
            app.repository.checkAllLinks()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

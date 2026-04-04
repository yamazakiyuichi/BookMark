package com.yamazaki.bookmark.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.yamazaki.bookmark.data.local.entity.BookmarkEntity

@Database(
    entities = [BookmarkEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BookmarkDatabase : RoomDatabase() {

    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: BookmarkDatabase? = null

        fun getInstance(context: Context): BookmarkDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BookmarkDatabase::class.java,
                    "bookmark_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

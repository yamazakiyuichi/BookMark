package com.yamazaki.bookmark.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yamazaki.bookmark.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks ORDER BY created_at DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE thumbnail_url IS NOT NULL AND thumbnail_url != '' ORDER BY created_at DESC")
    fun observeAllWithThumbnail(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY created_at DESC")
    suspend fun getAll(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun getById(id: Long): BookmarkEntity?

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BookmarkEntity): Long

    @Update
    suspend fun update(entity: BookmarkEntity)

    @Delete
    suspend fun delete(entity: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)
}

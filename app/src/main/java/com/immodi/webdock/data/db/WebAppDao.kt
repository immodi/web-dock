package com.immodi.webdock.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.immodi.webdock.domain.WebApp
import kotlinx.coroutines.flow.Flow

@Dao
interface WebAppDao {

    @Query("SELECT * FROM web_apps ORDER BY pinned DESC, sortOrder ASC")
    fun observeAll(): Flow<List<WebApp>>

    @Query("SELECT * FROM web_apps ORDER BY pinned DESC, sortOrder ASC")
    suspend fun getAllOnce(): List<WebApp>

    @Query("SELECT * FROM web_apps WHERE id = :id")
    suspend fun getById(id: String): WebApp?

    @Query("SELECT * FROM web_apps WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): WebApp?

    @Query("SELECT MAX(sortOrder) FROM web_apps")
    suspend fun maxSortOrder(): Int?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(app: WebApp)

    @Update
    suspend fun update(app: WebApp)

    @Delete
    suspend fun delete(app: WebApp)

    @Query("UPDATE web_apps SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int)

    @Query("UPDATE web_apps SET lastOpenedAt = :time WHERE id = :id")
    suspend fun updateLastOpenedAt(id: String, time: Long)
}

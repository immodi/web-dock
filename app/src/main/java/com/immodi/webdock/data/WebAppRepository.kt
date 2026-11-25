package com.immodi.webdock.data

import androidx.room.withTransaction
import com.immodi.webdock.data.db.WebAppDao
import com.immodi.webdock.data.db.WebDockDatabase
import com.immodi.webdock.domain.AppSettings
import com.immodi.webdock.domain.WebApp
import kotlinx.coroutines.flow.Flow

class WebAppRepository(
    private val database: WebDockDatabase,
    private val dao: WebAppDao,
) {

    val apps: Flow<List<WebApp>> = dao.observeAll()

    suspend fun get(id: String): WebApp? = dao.getById(id)

    suspend fun findDuplicate(url: String, excludeId: String? = null): WebApp? =
        dao.findByUrl(url)?.takeIf { it.id != excludeId }

    /**
     * Inserts or updates an app. Fails if the normalized URL already belongs to
     * another entry.
     */
    suspend fun save(app: WebApp, isNew: Boolean): Result<Unit> = runCatching {
        database.withTransaction {
            val existing = dao.findByUrl(app.url)
            if (existing != null && existing.id != app.id) {
                error("duplicate url")
            }
            if (isNew) {
                dao.insert(app.copy(sortOrder = nextSortOrder()))
            } else {
                dao.update(app)
            }
        }
    }

    suspend fun delete(id: String) {
        database.withTransaction {
            dao.getById(id)?.let { dao.delete(it) }
        }
    }

    suspend fun setPinned(id: String, pinned: Boolean) {
        database.withTransaction {
            val app = dao.getById(id) ?: return@withTransaction
            if (app.pinned == pinned) return@withTransaction
            dao.update(app.copy(pinned = pinned, sortOrder = nextSortOrder()))
        }
    }

    /** Moves an unpinned app within the unpinned group. Indexes are positions in that group. */
    suspend fun moveUnpinned(from: Int, to: Int) {
        database.withTransaction {
            val unpinned = dao.getAllOnce()
                .filterNot { it.pinned }
                .sortedBy { it.sortOrder }
            if (from !in unpinned.indices || to !in unpinned.indices || from == to) {
                return@withTransaction
            }
            val reordered = unpinned.toMutableList().apply { add(to, removeAt(from)) }
            reordered.forEachIndexed { index, app ->
                if (app.sortOrder != index) dao.updateSortOrder(app.id, index)
            }
        }
    }

    suspend fun markOpened(id: String) {
        dao.updateLastOpenedAt(id, System.currentTimeMillis())
    }

    /** Applies the global defaults to a new app. */
    fun newApp(
        url: String,
        name: String,
        nameEdited: Boolean,
        tags: List<String>,
        defaults: AppSettings,
    ): WebApp = WebApp(
        url = url,
        name = name,
        nameEdited = nameEdited,
        tags = tags,
        settings = defaults,
    )

    private suspend fun nextSortOrder(): Int = (dao.maxSortOrder() ?: -1) + 1
}

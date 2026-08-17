package com.example.data.local

import androidx.room.*
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentPage
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE isTrash = 0 ORDER BY updatedAt DESC")
    fun getAllActiveDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isTrash = 0 ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentDocuments(limit: Int = 10): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isTrash = 0 AND isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isTrash = 1 ORDER BY trashTimestamp DESC")
    fun getTrashDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isTrash = 0 AND category = :category ORDER BY updatedAt DESC")
    fun getDocumentsByCategory(category: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isTrash = 0 AND (title LIKE '%' || :query || '%' OR ocrText LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE id = :id")
    fun getDocumentFlow(id: Long): Flow<DocumentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("UPDATE documents SET isTrash = 1, trashTimestamp = :timestamp WHERE id = :id")
    suspend fun moveToTrash(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET isTrash = 0, trashTimestamp = 0 WHERE id = :id")
    suspend fun restoreFromTrash(id: Long)

    @Query("UPDATE documents SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM documents WHERE isTrash = 1")
    suspend fun purgeAllTrash()

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deletePermanently(id: Long)

    // Document Pages
    @Query("SELECT * FROM document_pages WHERE documentId = :documentId ORDER BY pageIndex ASC")
    fun getPagesForDocument(documentId: Long): Flow<List<DocumentPage>>

    @Query("SELECT * FROM document_pages WHERE documentId = :documentId ORDER BY pageIndex ASC")
    suspend fun getPagesListForDocument(documentId: Long): List<DocumentPage>

    @Query("SELECT * FROM document_pages WHERE id = :pageId")
    suspend fun getPageById(pageId: Long): DocumentPage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<DocumentPage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: DocumentPage): Long

    @Update
    suspend fun updatePage(page: DocumentPage)

    @Delete
    suspend fun deletePage(page: DocumentPage)

    @Query("DELETE FROM document_pages WHERE documentId = :documentId")
    suspend fun deletePagesForDocument(documentId: Long)
}

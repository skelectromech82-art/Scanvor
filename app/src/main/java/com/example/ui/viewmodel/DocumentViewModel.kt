package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.GscanDatabase
import com.example.data.model.DocumentCategory
import com.example.data.model.DocumentEntity
import com.example.data.preference.AppPreferences
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOrder(val displayName: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    SIZE_DESC("Largest Size"),
    PAGES_DESC("Most Pages")
}

data class DocumentUiState(
    val documents: List<DocumentEntity> = emptyList(),
    val recentDocuments: List<DocumentEntity> = emptyList(),
    val favoriteDocuments: List<DocumentEntity> = emptyList(),
    val trashDocuments: List<DocumentEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: DocumentCategory = DocumentCategory.ALL,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val selectedDocIds: Set<Long> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val totalDocumentsCount: Int = 0,
    val totalPagesCount: Int = 0,
    val totalStorageBytes: Long = 0L
)

class DocumentViewModel(application: Application) : AndroidViewModel(application) {
    private val database = GscanDatabase.getDatabase(application)
    val repository = DocumentRepository(application, database.documentDao())
    val preferences = AppPreferences(application)

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow(DocumentCategory.ALL)
    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    private val _selectedDocIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isLoading = MutableStateFlow(false)
    private val _userMessage = MutableStateFlow<String?>(null)

    private val dbDataFlow = combine(
        repository.activeDocuments,
        repository.favoriteDocuments,
        repository.trashDocuments
    ) { active, fav, trash ->
        Triple(active, fav, trash)
    }

    private val filterFlow = combine(
        _searchQuery,
        _selectedCategory,
        _sortOrder
    ) { query, cat, sort ->
        Triple(query, cat, sort)
    }

    private val uiStateExtraFlow = combine(
        _selectedDocIds,
        _isLoading,
        _userMessage
    ) { ids, loading, msg ->
        Triple(ids, loading, msg)
    }

    val uiState: StateFlow<DocumentUiState> = combine(
        dbDataFlow,
        filterFlow,
        uiStateExtraFlow
    ) { (activeDocs, favDocs, trashDocs), (query, category, sort), (selectedIds, loading, message) ->
        
        var filtered = if (query.isNotBlank()) {
            activeDocs.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.ocrText.contains(query, ignoreCase = true) ||
                it.tags.contains(query, ignoreCase = true)
            }
        } else {
            activeDocs
        }

        if (category != DocumentCategory.ALL) {
            filtered = filtered.filter { it.category == category.name }
        }

        val sorted = when (sort) {
            SortOrder.DATE_DESC -> filtered.sortedByDescending { it.updatedAt }
            SortOrder.DATE_ASC -> filtered.sortedBy { it.updatedAt }
            SortOrder.NAME_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortOrder.NAME_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            SortOrder.SIZE_DESC -> filtered.sortedByDescending { it.fileSize }
            SortOrder.PAGES_DESC -> filtered.sortedByDescending { it.pageCount }
        }

        val totalDocs = activeDocs.size
        val totalPages = activeDocs.sumOf { it.pageCount }
        val totalStorage = activeDocs.sumOf { it.fileSize }

        DocumentUiState(
            documents = sorted,
            recentDocuments = activeDocs.take(6),
            favoriteDocuments = favDocs,
            trashDocuments = trashDocs,
            searchQuery = query,
            selectedCategory = category,
            sortOrder = sort,
            selectedDocIds = selectedIds,
            isMultiSelectMode = selectedIds.isNotEmpty(),
            isSearching = query.isNotBlank(),
            isLoading = loading,
            userMessage = message,
            totalDocumentsCount = totalDocs,
            totalPagesCount = totalPages,
            totalStorageBytes = totalStorage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DocumentUiState()
    )

    fun refresh() {
        // Room Flows automatically emit updates upon database changes
    }

    fun updateDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.updateDocument(doc)
        }
    }


    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: DocumentCategory) {
        _selectedCategory.value = category
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun toggleDocumentSelection(docId: Long) {
        val current = _selectedDocIds.value.toMutableSet()
        if (current.contains(docId)) {
            current.remove(docId)
        } else {
            current.add(docId)
        }
        _selectedDocIds.value = current
    }

    fun selectAll() {
        val allIds = uiState.value.documents.map { it.id }.toSet()
        _selectedDocIds.value = allIds
    }

    fun clearSelection() {
        _selectedDocIds.value = emptySet()
    }

    fun toggleFavorite(documentId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(documentId, isFavorite)
        }
    }

    fun moveToTrash(documentId: Long) {
        viewModelScope.launch {
            repository.moveToTrash(documentId)
            _userMessage.value = "Document moved to Trash"
        }
    }

    fun deleteSelectedToTrash() {
        viewModelScope.launch {
            val ids = _selectedDocIds.value.toList()
            ids.forEach { repository.moveToTrash(it) }
            clearSelection()
            _userMessage.value = "${ids.size} document(s) moved to Trash"
        }
    }

    fun restoreFromTrash(documentId: Long) {
        viewModelScope.launch {
            repository.restoreFromTrash(documentId)
            _userMessage.value = "Document restored"
        }
    }

    fun deletePermanently(documentId: Long) {
        viewModelScope.launch {
            repository.deletePermanently(documentId)
            _userMessage.value = "Document permanently deleted"
        }
    }

    fun purgeTrash() {
        viewModelScope.launch {
            repository.purgeTrash()
            _userMessage.value = "Trash emptied"
        }
    }

    fun importPdf(uri: Uri, title: String? = null, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val docId = repository.importPdf(uri, title)
                _userMessage.value = "PDF imported successfully"
                onComplete(docId)
            } catch (e: Exception) {
                _userMessage.value = "Failed to import PDF: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importImages(uris: List<Uri>, title: String, category: String, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val docId = repository.importImagesAsDocument(uris, title, category)
                if (docId > 0) {
                    _userMessage.value = "Images imported into new document"
                    onComplete(docId)
                }
            } catch (e: Exception) {
                _userMessage.value = "Import failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun mergeSelectedDocuments(mergedTitle: String, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val ids = _selectedDocIds.value.toList()
                val newId = repository.mergeDocuments(ids, mergedTitle)
                clearSelection()
                _userMessage.value = "Documents merged successfully"
                onComplete(newId)
            } catch (e: Exception) {
                _userMessage.value = "Merge failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}

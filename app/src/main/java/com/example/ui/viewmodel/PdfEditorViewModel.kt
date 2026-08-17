package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.GscanDatabase
import com.example.data.model.*
import com.example.data.repository.DocumentRepository
import com.example.utils.ImageProcessingEngine
import com.example.utils.OcrEngine
import com.example.utils.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class EditorToolMode {
    VIEW,
    ANNOTATE_TEXT,
    DRAW,
    HIGHLIGHT,
    SIGNATURE,
    STAMPS,
    WATERMARK,
    PAGE_ORGANIZER
}

data class DrawingPath(
    val points: List<Pair<Float, Float>>,
    val colorArgb: Int,
    val strokeWidth: Float,
    val isHighlighter: Boolean = false
)

data class PdfEditorUiState(
    val document: DocumentEntity? = null,
    val pages: List<DocumentPage> = emptyList(),
    val pageBitmaps: Map<Long, Bitmap> = emptyMap(),
    val activePageIndex: Int = 0,
    val toolMode: EditorToolMode = EditorToolMode.VIEW,
    val isLoading: Boolean = false,
    val isProcessingOcr: Boolean = false,
    val ocrResultText: String = "",
    val activeColor: Int = 0xFF1E40AF.toInt(),
    val activeStrokeWidth: Float = 6f,
    val drawingPaths: Map<Int, List<DrawingPath>> = emptyMap(), // pageIndex -> paths
    val annotations: Map<Int, List<AnnotationItem>> = emptyMap(), // pageIndex -> items
    val watermarkText: String = "",
    val isPasswordModalOpen: Boolean = false,
    val isWatermarkModalOpen: Boolean = false,
    val isSignatureModalOpen: Boolean = false,
    val isAddTextModalOpen: Boolean = false,
    val isCompressModalOpen: Boolean = false,
    val isSplitModalOpen: Boolean = false,
    val userMessage: String? = null,
    val isDocumentUnlocked: Boolean = true
)

class PdfEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val database = GscanDatabase.getDatabase(application)
    val repository = DocumentRepository(application, database.documentDao())

    private val _uiState = MutableStateFlow(PdfEditorUiState())
    val uiState: StateFlow<PdfEditorUiState> = _uiState.asStateFlow()

    private var currentDocumentId: Long = 0

    fun loadDocument(documentId: Long) {
        currentDocumentId = documentId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val doc = repository.getDocumentById(documentId)
            if (doc != null) {
                val pages = repository.getPagesList(documentId)
                val isUnlocked = !doc.isPasswordProtected

                val bitmaps = mutableMapOf<Long, Bitmap>()
                withContext(Dispatchers.IO) {
                    pages.forEach { page ->
                        val file = File(page.imagePath)
                        if (file.exists()) {
                            val bmp = BitmapFactory.decodeFile(file.absolutePath)
                            if (bmp != null) {
                                bitmaps[page.id] = bmp
                            }
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        document = doc,
                        pages = pages,
                        pageBitmaps = bitmaps,
                        activePageIndex = 0,
                        isDocumentUnlocked = isUnlocked,
                        watermarkText = doc.watermarkText ?: "",
                        ocrResultText = doc.ocrText,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, userMessage = "Document not found") }
            }
        }
    }

    fun selectPageIndex(index: Int) {
        if (index in _uiState.value.pages.indices) {
            _uiState.update { it.copy(activePageIndex = index) }
        }
    }

    fun setToolMode(mode: EditorToolMode) {
        _uiState.update { it.copy(toolMode = mode) }
    }

    fun setActiveColor(colorArgb: Int) {
        _uiState.update { it.copy(activeColor = colorArgb) }
    }

    fun setActiveStrokeWidth(width: Float) {
        _uiState.update { it.copy(activeStrokeWidth = width) }
    }

    fun addDrawingPath(pageIndex: Int, path: DrawingPath) {
        _uiState.update { current ->
            val map = current.drawingPaths.toMutableMap()
            val list = (map[pageIndex] ?: emptyList()) + path
            map[pageIndex] = list
            current.copy(drawingPaths = map)
        }
    }

    fun clearDrawingsForCurrentPage() {
        val pageIdx = _uiState.value.activePageIndex
        _uiState.update { current ->
            val map = current.drawingPaths.toMutableMap()
            map.remove(pageIdx)
            current.copy(drawingPaths = map)
        }
    }

    fun addAnnotationToCurrentPage(annotation: AnnotationItem) {
        val pageIdx = _uiState.value.activePageIndex
        _uiState.update { current ->
            val map = current.annotations.toMutableMap()
            val list = (map[pageIdx] ?: emptyList()) + annotation
            map[pageIdx] = list
            current.copy(annotations = map)
        }
    }

    fun removeAnnotation(pageIndex: Int, annotationId: String) {
        _uiState.update { current ->
            val map = current.annotations.toMutableMap()
            val list = (map[pageIndex] ?: emptyList()).filter { it.id != annotationId }
            map[pageIndex] = list
            current.copy(annotations = map)
        }
    }

    fun rotateCurrentPage() {
        val pages = _uiState.value.pages
        val activeIdx = _uiState.value.activePageIndex
        if (activeIdx in pages.indices) {
            val targetPage = pages[activeIdx]
            val newRot = (targetPage.rotationDegrees + 90) % 360
            val updatedPage = targetPage.copy(rotationDegrees = newRot)

            viewModelScope.launch {
                repository.updatePage(updatedPage)
                repository.rebuildDocumentPdf(currentDocumentId)
                loadDocument(currentDocumentId)
            }
        }
    }

    fun duplicateCurrentPage() {
        val pages = _uiState.value.pages
        val activeIdx = _uiState.value.activePageIndex
        if (activeIdx in pages.indices) {
            val page = pages[activeIdx]
            viewModelScope.launch {
                // Copy image file
                val context = getApplication<Application>()
                val origFile = File(page.imagePath)
                if (origFile.exists()) {
                    val bmp = BitmapFactory.decodeFile(origFile.absolutePath)
                    val newName = "page_copy_${System.currentTimeMillis()}"
                    val newPath = ImageProcessingEngine.saveBitmapToFile(context, bmp, newName)

                    val newPage = DocumentPage(
                        documentId = currentDocumentId,
                        pageIndex = pages.size,
                        imagePath = newPath,
                        filterType = page.filterType,
                        rotationDegrees = page.rotationDegrees
                    )
                    database.documentDao().insertPage(newPage)
                    repository.rebuildDocumentPdf(currentDocumentId)
                    loadDocument(currentDocumentId)
                    _uiState.update { it.copy(userMessage = "Page duplicated") }
                }
            }
        }
    }

    fun deleteCurrentPage() {
        val pages = _uiState.value.pages
        val activeIdx = _uiState.value.activePageIndex
        if (pages.size <= 1) {
            _uiState.update { it.copy(userMessage = "Cannot delete the only page in the document") }
            return
        }
        if (activeIdx in pages.indices) {
            val page = pages[activeIdx]
            viewModelScope.launch {
                repository.deletePage(page, currentDocumentId)
                loadDocument(currentDocumentId)
                _uiState.update { it.copy(userMessage = "Page deleted") }
            }
        }
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        val pages = _uiState.value.pages.toMutableList()
        if (fromIndex in pages.indices && toIndex in pages.indices) {
            val item = pages.removeAt(fromIndex)
            pages.add(toIndex, item)
            viewModelScope.launch {
                pages.forEachIndexed { index, page ->
                    database.documentDao().updatePage(page.copy(pageIndex = index))
                }
                repository.rebuildDocumentPdf(currentDocumentId)
                loadDocument(currentDocumentId)
            }
        }
    }

    fun applyFilterToCurrentPage(filter: FilterType) {
        val pages = _uiState.value.pages
        val activeIdx = _uiState.value.activePageIndex
        if (activeIdx in pages.indices) {
            val page = pages[activeIdx]
            viewModelScope.launch {
                database.documentDao().updatePage(page.copy(filterType = filter.name))
                repository.rebuildDocumentPdf(currentDocumentId)
                loadDocument(currentDocumentId)
            }
        }
    }

    fun updateWatermark(text: String) {
        val doc = _uiState.value.document ?: return
        viewModelScope.launch {
            val updated = doc.copy(watermarkText = text.ifBlank { null })
            repository.updateDocument(updated)
            repository.rebuildDocumentPdf(currentDocumentId)
            _uiState.update { it.copy(document = updated, watermarkText = text, isWatermarkModalOpen = false, userMessage = "Watermark updated") }
        }
    }

    fun setPasswordProtection(password: String?) {
        val doc = _uiState.value.document ?: return
        viewModelScope.launch {
            val isProtected = !password.isNullOrBlank()
            val updated = doc.copy(
                isPasswordProtected = isProtected,
                passwordHash = password
            )
            repository.updateDocument(updated)
            _uiState.update {
                it.copy(
                    document = updated,
                    isPasswordModalOpen = false,
                    isDocumentUnlocked = true,
                    userMessage = if (isProtected) "Password protection enabled" else "Password removed"
                )
            }
        }
    }

    fun unlockDocument(passwordAttempt: String): Boolean {
        val doc = _uiState.value.document ?: return true
        if (!doc.isPasswordProtected) return true

        if (doc.passwordHash == passwordAttempt) {
            _uiState.update { it.copy(isDocumentUnlocked = true, userMessage = "Document unlocked") }
            return true
        } else {
            _uiState.update { it.copy(userMessage = "Incorrect password") }
            return false
        }
    }

    fun compressDocument(quality: CompressionQuality) {
        val doc = _uiState.value.document ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val updated = doc.copy(quality = quality.name)
            repository.updateDocument(updated)
            repository.rebuildDocumentPdf(currentDocumentId)
            loadDocument(currentDocumentId)
            _uiState.update { it.copy(isCompressModalOpen = false, isLoading = false, userMessage = "Document compressed (${quality.displayName})") }
        }
    }

    fun performOcrOnCurrentPage() {
        val pages = _uiState.value.pages
        val activeIdx = _uiState.value.activePageIndex
        if (activeIdx !in pages.indices) return

        val page = pages[activeIdx]
        val bmp = _uiState.value.pageBitmaps[page.id] ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingOcr = true) }
            try {
                val extractedText = OcrEngine.recognizeText(bmp)
                _uiState.update {
                    it.copy(
                        isProcessingOcr = false,
                        ocrResultText = extractedText,
                        userMessage = "OCR text extracted successfully"
                    )
                }

                // Update in database
                val doc = _uiState.value.document
                if (doc != null) {
                    val combinedOcr = if (doc.ocrText.isBlank()) extractedText else "${doc.ocrText}\n\n--- Page ${activeIdx + 1} ---\n$extractedText"
                    repository.updateDocument(doc.copy(ocrText = combinedOcr))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isProcessingOcr = false, userMessage = "OCR error: ${e.message}")
                }
            }
        }
    }

    fun splitDocument(selectedPages: List<Int>, newDocTitle: String, onComplete: (Long) -> Unit) {
        val pages = _uiState.value.pages
        val selectedPageEntities = selectedPages.mapNotNull { if (it in pages.indices) pages[it] else null }
        if (selectedPageEntities.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val context = getApplication<Application>()
                val pageData = mutableListOf<Pair<Bitmap, FilterType>>()

                selectedPageEntities.forEach { page ->
                    val file = File(page.imagePath)
                    if (file.exists()) {
                        val bmp = BitmapFactory.decodeFile(file.absolutePath)
                        if (bmp != null) {
                            val filter = try { FilterType.valueOf(page.filterType) } catch (e: Exception) { FilterType.ORIGINAL }
                            val rot = if (page.rotationDegrees != 0) ImageProcessingEngine.rotateBitmap(bmp, page.rotationDegrees.toFloat()) else bmp
                            pageData.add(Pair(rot, filter))
                        }
                    }
                }

                val newDocId = repository.createDocumentFromPages(
                    title = newDocTitle.ifBlank { "Split_${System.currentTimeMillis()}" },
                    category = _uiState.value.document?.category ?: DocumentCategory.WORK.name,
                    pagesData = pageData
                )

                _uiState.update { it.copy(isLoading = false, isSplitModalOpen = false, userMessage = "Created new PDF from extracted pages") }
                onComplete(newDocId)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Split failed: ${e.message}") }
            }
        }
    }

    fun updateDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.updateDocument(doc)
            _uiState.update { it.copy(document = doc) }
        }
    }

    fun setPasswordModalOpen(open: Boolean) = _uiState.update { it.copy(isPasswordModalOpen = open) }
    fun setWatermarkModalOpen(open: Boolean) = _uiState.update { it.copy(isWatermarkModalOpen = open) }
    fun setSignatureModalOpen(open: Boolean) = _uiState.update { it.copy(isSignatureModalOpen = open) }
    fun setAddTextModalOpen(open: Boolean) = _uiState.update { it.copy(isAddTextModalOpen = open) }
    fun setCompressModalOpen(open: Boolean) = _uiState.update { it.copy(isCompressModalOpen = open) }
    fun setSplitModalOpen(open: Boolean) = _uiState.update { it.copy(isSplitModalOpen = open) }

    fun clearUserMessage() = _uiState.update { it.copy(userMessage = null) }
}

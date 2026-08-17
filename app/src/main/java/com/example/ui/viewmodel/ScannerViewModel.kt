package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.GscanDatabase
import com.example.data.model.*
import com.example.data.preference.AppPreferences
import com.example.data.repository.DocumentRepository
import com.example.utils.ImageProcessingEngine
import com.example.utils.OcrEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScannedPageItem(
    val originalBitmap: Bitmap,
    val filterType: FilterType = FilterType.MAGIC_COLOR,
    val rotationDegrees: Int = 0,
    val cropPoints: CropCornerPoints = CropCornerPoints(),
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val isCropped: Boolean = false
)

enum class FlashMode(val iconName: String) {
    OFF("Off"),
    AUTO("Auto"),
    ON("On"),
    TORCH("Torch")
}

data class ScannerUiState(
    val scannedPages: List<ScannedPageItem> = emptyList(),
    val currentPageIndex: Int = 0,
    val flashMode: FlashMode = FlashMode.OFF,
    val cameraLensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val isAutoCaptureEnabled: Boolean = false,
    val isFlashSupported: Boolean = true,
    val isProcessing: Boolean = false,
    val showCropLoupe: Boolean = false,
    val activeLoupePoint: Int = -1, // 0: TL, 1: TR, 2: BR, 3: BL
    val saveDocumentTitle: String = "",
    val saveCategory: DocumentCategory = DocumentCategory.PERSONAL,
    val savePageFormat: PageFormat = PageFormat.A4,
    val saveQuality: CompressionQuality = CompressionQuality.HIGH,
    val watermarkText: String = "",
    val errorMessage: String? = null,
    val successDocId: Long? = null
)

class ScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = GscanDatabase.getDatabase(application)
    private val repository = DocumentRepository(application, database.documentDao())
    val preferences = AppPreferences(application)

    private val _uiState = MutableStateFlow(
        ScannerUiState(
            saveDocumentTitle = "Gscan_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())}"
        )
    )
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onImageCaptured(rawBitmap: Bitmap) {
        val detectedCorners = ImageProcessingEngine.detectDocumentCorners(rawBitmap)
        val defaultFilter = try {
            FilterType.valueOf(preferences.defaultFilter.value)
        } catch (e: Exception) {
            FilterType.MAGIC_COLOR
        }

        val newPage = ScannedPageItem(
            originalBitmap = rawBitmap,
            filterType = defaultFilter,
            cropPoints = detectedCorners
        )

        _uiState.update { current ->
            val updated = current.scannedPages + newPage
            current.copy(
                scannedPages = updated,
                currentPageIndex = updated.size - 1
            )
        }
    }

    fun selectPageIndex(index: Int) {
        if (index in _uiState.value.scannedPages.indices) {
            _uiState.update { it.copy(currentPageIndex = index) }
        }
    }

    fun setFilterForCurrentPage(filter: FilterType) {
        val idx = _uiState.value.currentPageIndex
        if (idx in _uiState.value.scannedPages.indices) {
            _uiState.update { current ->
                val list = current.scannedPages.toMutableList()
                list[idx] = list[idx].copy(filterType = filter)
                current.copy(scannedPages = list)
            }
        }
    }

    fun rotateCurrentPage() {
        val idx = _uiState.value.currentPageIndex
        if (idx in _uiState.value.scannedPages.indices) {
            _uiState.update { current ->
                val list = current.scannedPages.toMutableList()
                val newRot = (list[idx].rotationDegrees + 90) % 360
                list[idx] = list[idx].copy(rotationDegrees = newRot)
                current.copy(scannedPages = list)
            }
        }
    }

    fun updateCropCorner(pointIndex: Int, normalizedX: Float, normalizedY: Float) {
        val idx = _uiState.value.currentPageIndex
        if (idx in _uiState.value.scannedPages.indices) {
            _uiState.update { current ->
                val list = current.scannedPages.toMutableList()
                val oldPoints = list[idx].cropPoints
                val newPoints = when (pointIndex) {
                    0 -> oldPoints.copy(topLeftX = normalizedX.coerceIn(0f, 0.9f), topLeftY = normalizedY.coerceIn(0f, 0.9f))
                    1 -> oldPoints.copy(topRightX = normalizedX.coerceIn(0.1f, 1f), topRightY = normalizedY.coerceIn(0f, 0.9f))
                    2 -> oldPoints.copy(bottomRightX = normalizedX.coerceIn(0.1f, 1f), bottomRightY = normalizedY.coerceIn(0.1f, 1f))
                    3 -> oldPoints.copy(bottomLeftX = normalizedX.coerceIn(0f, 0.9f), bottomLeftY = normalizedY.coerceIn(0.1f, 1f))
                    else -> oldPoints
                }
                list[idx] = list[idx].copy(cropPoints = newPoints, isCropped = true)
                current.copy(scannedPages = list, activeLoupePoint = pointIndex, showCropLoupe = true)
            }
        }
    }

    fun hideLoupe() {
        _uiState.update { it.copy(showCropLoupe = false, activeLoupePoint = -1) }
    }

    fun resetCrop() {
        val idx = _uiState.value.currentPageIndex
        if (idx in _uiState.value.scannedPages.indices) {
            _uiState.update { current ->
                val list = current.scannedPages.toMutableList()
                list[idx] = list[idx].copy(cropPoints = CropCornerPoints(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f), isCropped = false)
                current.copy(scannedPages = list)
            }
        }
    }

    fun retakeCurrentPage() {
        val idx = _uiState.value.currentPageIndex
        if (idx in _uiState.value.scannedPages.indices) {
            _uiState.update { current ->
                val list = current.scannedPages.toMutableList()
                list.removeAt(idx)
                val newIndex = (idx - 1).coerceAtLeast(0)
                current.copy(scannedPages = list, currentPageIndex = newIndex)
            }
        }
    }

    fun deleteCurrentPage() {
        retakeCurrentPage()
    }

    fun toggleFlashMode() {
        _uiState.update { current ->
            val next = when (current.flashMode) {
                FlashMode.OFF -> FlashMode.AUTO
                FlashMode.AUTO -> FlashMode.ON
                FlashMode.ON -> FlashMode.TORCH
                FlashMode.TORCH -> FlashMode.OFF
            }
            current.copy(flashMode = next)
        }
    }

    fun toggleCameraFacing() {
        _uiState.update { current ->
            val next = if (current.cameraLensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            current.copy(cameraLensFacing = next)
        }
    }

    fun toggleAutoCapture() {
        _uiState.update { it.copy(isAutoCaptureEnabled = !it.isAutoCaptureEnabled) }
    }

    fun setDocumentTitle(title: String) {
        _uiState.update { it.copy(saveDocumentTitle = title) }
    }

    fun setCategory(category: DocumentCategory) {
        _uiState.update { it.copy(saveCategory = category) }
    }

    fun setPageFormat(format: PageFormat) {
        _uiState.update { it.copy(savePageFormat = format) }
    }

    fun setCompressionQuality(quality: CompressionQuality) {
        _uiState.update { it.copy(saveQuality = quality) }
    }

    fun setWatermarkText(watermark: String) {
        _uiState.update { it.copy(watermarkText = watermark) }
    }

    fun saveDocument(onComplete: (Long) -> Unit) {
        val state = _uiState.value
        if (state.scannedPages.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val preparedPages = state.scannedPages.map { page ->
                    var bmp = page.originalBitmap
                    if (page.isCropped) {
                        bmp = ImageProcessingEngine.cropPerspective(bmp, page.cropPoints)
                    }
                    if (page.rotationDegrees != 0) {
                        bmp = ImageProcessingEngine.rotateBitmap(bmp, page.rotationDegrees.toFloat())
                    }
                    if (page.brightness != 0f || page.contrast != 0f) {
                        bmp = ImageProcessingEngine.adjustImage(bmp, page.brightness, page.contrast)
                    }
                    Pair(bmp, page.filterType)
                }

                val docId = repository.createDocumentFromPages(
                    title = state.saveDocumentTitle.ifBlank { "Gscan_${System.currentTimeMillis()}" },
                    category = state.saveCategory.name,
                    pagesData = preparedPages,
                    pageFormat = state.savePageFormat,
                    quality = state.saveQuality,
                    watermarkText = state.watermarkText.ifBlank { null }
                )

                _uiState.update { it.copy(isProcessing = false, successDocId = docId) }
                onComplete(docId)
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, errorMessage = "Save failed: ${e.message}") }
            }
        }
    }

    fun resetScanner() {
        clearAll()
    }

    fun clearAll() {
        _uiState.value = ScannerUiState(
            saveDocumentTitle = "Gscan_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())}"
        )
    }
}


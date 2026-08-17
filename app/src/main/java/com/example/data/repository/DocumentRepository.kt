package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.local.DocumentDao
import com.example.data.model.*
import com.example.utils.ImageProcessingEngine
import com.example.utils.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DocumentRepository(
    private val context: Context,
    private val documentDao: DocumentDao
) {
    val activeDocuments: Flow<List<DocumentEntity>> = documentDao.getAllActiveDocuments()
    val favoriteDocuments: Flow<List<DocumentEntity>> = documentDao.getFavoriteDocuments()
    val trashDocuments: Flow<List<DocumentEntity>> = documentDao.getTrashDocuments()

    fun getRecentDocuments(limit: Int = 10): Flow<List<DocumentEntity>> =
        documentDao.getRecentDocuments(limit)

    fun getDocumentsByCategory(category: String): Flow<List<DocumentEntity>> =
        documentDao.getDocumentsByCategory(category)

    fun searchDocuments(query: String): Flow<List<DocumentEntity>> =
        documentDao.searchDocuments(query)

    fun getDocumentFlow(id: Long): Flow<DocumentEntity?> =
        documentDao.getDocumentFlow(id)

    suspend fun getDocumentById(id: Long): DocumentEntity? =
        documentDao.getDocumentById(id)

    fun getPagesForDocument(documentId: Long): Flow<List<DocumentPage>> =
        documentDao.getPagesForDocument(documentId)

    suspend fun getPagesList(documentId: Long): List<DocumentPage> =
        documentDao.getPagesListForDocument(documentId)

    /**
     * Saves a new scanned document with its processed pages and generates the PDF
     */
    suspend fun createDocumentFromPages(
        title: String,
        category: String,
        pagesData: List<Pair<Bitmap, FilterType>>,
        pageFormat: PageFormat = PageFormat.A4,
        quality: CompressionQuality = CompressionQuality.HIGH,
        watermarkText: String? = null,
        ocrTextCombined: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val cleanTitle = if (title.isBlank()) "Gscan_${timestamp}" else title

        // 1. Process and save page images
        val pageBitmaps = mutableListOf<Bitmap>()
        val pageEntities = mutableListOf<DocumentPage>()
        var firstThumbnailPath = ""

        for (i in pagesData.indices) {
            val (bmp, filter) = pagesData[i]
            val processedBmp = ImageProcessingEngine.applyFilter(bmp, filter)
            pageBitmaps.add(processedBmp)

            val pageFileName = "page_${timestamp}_${i}"
            val savedImagePath = ImageProcessingEngine.saveBitmapToFile(context, processedBmp, pageFileName)

            if (i == 0) {
                firstThumbnailPath = ImageProcessingEngine.generateThumbnail(context, processedBmp, "doc_${timestamp}")
            }

            pageEntities.add(
                DocumentPage(
                    documentId = 0, // Assigned after document insertion
                    pageIndex = i,
                    imagePath = savedImagePath,
                    filterType = filter.name,
                    rotationDegrees = 0
                )
            )
        }

        // 2. Generate PDF file
        val pdfFile = PdfEngine.createPdfFromBitmaps(
            context = context,
            bitmaps = pageBitmaps,
            outputFileName = cleanTitle,
            pageFormat = pageFormat,
            quality = quality,
            watermarkText = watermarkText
        )

        // 3. Insert Document Entity into Database
        val docEntity = DocumentEntity(
            title = cleanTitle,
            pageCount = pageBitmaps.size,
            fileSize = pdfFile.length(),
            createdAt = timestamp,
            updatedAt = timestamp,
            category = category,
            pdfFilePath = pdfFile.absolutePath,
            thumbnailPath = firstThumbnailPath,
            pageSize = pageFormat.name,
            quality = quality.name,
            watermarkText = watermarkText,
            ocrText = ocrTextCombined
        )

        val docId = documentDao.insertDocument(docEntity)

        // 4. Insert pages with assigned docId
        val finalPages = pageEntities.map { it.copy(documentId = docId) }
        documentDao.insertPages(finalPages)

        docId
    }

    /**
     * Import a PDF file from external Uri and add to documents
     */
    suspend fun importPdf(uri: Uri, title: String? = null): Long = withContext(Dispatchers.IO) {
        val importedFile = PdfEngine.importPdfFromUri(context, uri, title)
        val renderedBitmaps = PdfEngine.renderPdfToBitmaps(context, importedFile)

        val timestamp = System.currentTimeMillis()
        val docTitle = title ?: importedFile.nameWithoutExtension
        var firstThumbnailPath = ""

        val pageEntities = mutableListOf<DocumentPage>()
        for (i in renderedBitmaps.indices) {
            val bmp = renderedBitmaps[i]
            val pageFileName = "page_import_${timestamp}_${i}"
            val savedImagePath = ImageProcessingEngine.saveBitmapToFile(context, bmp, pageFileName)

            if (i == 0) {
                firstThumbnailPath = ImageProcessingEngine.generateThumbnail(context, bmp, "doc_${timestamp}")
            }

            pageEntities.add(
                DocumentPage(
                    documentId = 0,
                    pageIndex = i,
                    imagePath = savedImagePath,
                    filterType = FilterType.ORIGINAL.name
                )
            )
        }

        val docEntity = DocumentEntity(
            title = docTitle,
            pageCount = if (renderedBitmaps.isNotEmpty()) renderedBitmaps.size else 1,
            fileSize = importedFile.length(),
            createdAt = timestamp,
            updatedAt = timestamp,
            category = DocumentCategory.WORK.name,
            pdfFilePath = importedFile.absolutePath,
            thumbnailPath = firstThumbnailPath
        )

        val docId = documentDao.insertDocument(docEntity)
        val finalPages = pageEntities.map { it.copy(documentId = docId) }
        documentDao.insertPages(finalPages)

        docId
    }

    /**
     * Import multiple images from gallery and convert to a new document
     */
    suspend fun importImagesAsDocument(
        imageUris: List<Uri>,
        title: String = "Gallery Scan",
        category: String = DocumentCategory.PERSONAL.name
    ): Long = withContext(Dispatchers.IO) {
        val pagesData = mutableListOf<Pair<Bitmap, FilterType>>()

        for (uri in imageUris) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                if (bmp != null) {
                    pagesData.add(Pair(bmp, FilterType.MAGIC_COLOR))
                }
            }
        }

        if (pagesData.isEmpty()) return@withContext -1L

        createDocumentFromPages(
            title = title,
            category = category,
            pagesData = pagesData
        )
    }

    /**
     * Merge multiple documents into a single new document
     */
    suspend fun mergeDocuments(documentIds: List<Long>, mergedTitle: String): Long = withContext(Dispatchers.IO) {
        val allPagesData = mutableListOf<Pair<Bitmap, FilterType>>()

        for (docId in documentIds) {
            val pages = documentDao.getPagesListForDocument(docId)
            for (page in pages) {
                val file = File(page.imagePath)
                if (file.exists()) {
                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                    if (bmp != null) {
                        val filter = try { FilterType.valueOf(page.filterType) } catch (e: Exception) { FilterType.ORIGINAL }
                        val rotatedBmp = if (page.rotationDegrees != 0) {
                            ImageProcessingEngine.rotateBitmap(bmp, page.rotationDegrees.toFloat())
                        } else bmp
                        allPagesData.add(Pair(rotatedBmp, filter))
                    }
                }
            }
        }

        if (allPagesData.isEmpty()) return@withContext -1L

        createDocumentFromPages(
            title = mergedTitle,
            category = DocumentCategory.WORK.name,
            pagesData = allPagesData
        )
    }

    /**
     * Update an existing document (title, category, OCR text, password, watermark)
     */
    suspend fun updateDocument(document: DocumentEntity) = withContext(Dispatchers.IO) {
        documentDao.updateDocument(document.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * Rebuild and re-save document PDF after page alterations (delete, reorder, rotate, filter)
     */
    suspend fun rebuildDocumentPdf(documentId: Long) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(documentId) ?: return@withContext
        val pages = documentDao.getPagesListForDocument(documentId)
        val bitmaps = mutableListOf<Bitmap>()

        for (page in pages) {
            val file = File(page.imagePath)
            if (file.exists()) {
                val rawBmp = BitmapFactory.decodeFile(file.absolutePath)
                if (rawBmp != null) {
                    val filter = try { FilterType.valueOf(page.filterType) } catch (e: Exception) { FilterType.ORIGINAL }
                    var processed = ImageProcessingEngine.applyFilter(rawBmp, filter)
                    if (page.rotationDegrees != 0) {
                        processed = ImageProcessingEngine.rotateBitmap(processed, page.rotationDegrees.toFloat())
                    }
                    bitmaps.add(processed)
                }
            }
        }

        if (bitmaps.isEmpty()) return@withContext

        val pageFormat = try { PageFormat.valueOf(doc.pageSize) } catch (e: Exception) { PageFormat.A4 }
        val quality = try { CompressionQuality.valueOf(doc.quality) } catch (e: Exception) { CompressionQuality.HIGH }

        val pdfFile = PdfEngine.createPdfFromBitmaps(
            context = context,
            bitmaps = bitmaps,
            outputFileName = doc.title,
            pageFormat = pageFormat,
            quality = quality,
            watermarkText = doc.watermarkText
        )

        val updatedDoc = doc.copy(
            pageCount = bitmaps.size,
            fileSize = pdfFile.length(),
            pdfFilePath = pdfFile.absolutePath,
            updatedAt = System.currentTimeMillis()
        )
        documentDao.updateDocument(updatedDoc)
    }

    suspend fun toggleFavorite(documentId: Long, isFavorite: Boolean) {
        documentDao.toggleFavorite(documentId, isFavorite)
    }

    suspend fun moveToTrash(documentId: Long) {
        documentDao.moveToTrash(documentId)
    }

    suspend fun restoreFromTrash(documentId: Long) {
        documentDao.restoreFromTrash(documentId)
    }

    suspend fun deletePermanently(documentId: Long) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(documentId)
        if (doc != null) {
            // Delete physical files
            File(doc.pdfFilePath).delete()
            File(doc.thumbnailPath).delete()
            val pages = documentDao.getPagesListForDocument(documentId)
            for (p in pages) {
                File(p.imagePath).delete()
            }
            documentDao.deletePagesForDocument(documentId)
            documentDao.deletePermanently(documentId)
        }
    }

    suspend fun purgeTrash() = withContext(Dispatchers.IO) {
        documentDao.purgeAllTrash()
    }

    suspend fun updatePage(page: DocumentPage) {
        documentDao.updatePage(page)
    }

    suspend fun deletePage(page: DocumentPage, docId: Long) = withContext(Dispatchers.IO) {
        documentDao.deletePage(page)
        File(page.imagePath).delete()
        rebuildDocumentPdf(docId)
    }
}

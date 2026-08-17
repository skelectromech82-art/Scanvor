package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FilterType(val displayName: String) {
    ORIGINAL("Original"),
    MAGIC_COLOR("Magic Color"),
    BW("B & W"),
    GRAYSCALE("Grayscale"),
    HIGH_CONTRAST("High Contrast"),
    WARM("Warm")
}

enum class PageFormat(val displayName: String, val widthPt: Int, val heightPt: Int) {
    A4("A4 (210 × 297 mm)", 595, 842),
    LETTER("US Letter (8.5 × 11 in)", 612, 792),
    ORIGINAL("Original Aspect Ratio", 0, 0)
}

enum class CompressionQuality(val displayName: String, val jpegQuality: Int, val scale: Float) {
    HIGH("High Quality (Original)", 92, 1.0f),
    MEDIUM("Standard (Balanced)", 75, 0.8f),
    COMPACT("Small Size (Email/Chat)", 50, 0.6f)
}

enum class DocumentCategory(val displayName: String) {
    ALL("All"),
    WORK("Work"),
    PERSONAL("Personal"),
    RECEIPTS("Receipts"),
    ID_CARDS("ID & Cards"),
    INVOICES("Invoices"),
    LEGAL("Legal"),
    NOTES("Notes"),
    BOOKS("Books & Study")
}

data class CropCornerPoints(
    val topLeftX: Float = 0.05f,
    val topLeftY: Float = 0.05f,
    val topRightX: Float = 0.95f,
    val topRightY: Float = 0.05f,
    val bottomRightX: Float = 0.95f,
    val bottomRightY: Float = 0.95f,
    val bottomLeftX: Float = 0.05f,
    val bottomLeftY: Float = 0.95f
)

enum class AnnotationType {
    TEXT,
    SIGNATURE,
    STAMP,
    DRAWING,
    HIGHLIGHT,
    SHAPE
}

data class AnnotationItem(
    val id: String,
    val type: AnnotationType = AnnotationType.TEXT,
    val content: String = "", // text string or stamp label
    val xRatio: Float = 0.5f,
    val yRatio: Float = 0.5f,
    val widthRatio: Float = 0.3f,
    val heightRatio: Float = 0.1f,
    val colorArgb: Int = 0xFF1E40AF.toInt(),
    val strokeWidth: Float = 4f,
    val pointsData: String = "", // serialized path points "x1,y1;x2,y2;..."
    val rotation: Float = 0f,
    val fontSizeSp: Float = 16f,
    val signaturePath: String = ""
)

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val pageCount: Int = 1,
    val fileSize: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isTrash: Boolean = false,
    val trashTimestamp: Long = 0,
    val category: String = DocumentCategory.PERSONAL.name,
    val pdfFilePath: String = "",
    val thumbnailPath: String = "",
    val tags: String = "",
    val ocrText: String = "",
    val pageSize: String = PageFormat.A4.name,
    val quality: String = CompressionQuality.HIGH.name,
    val isPasswordProtected: Boolean = false,
    val passwordHash: String? = null,
    val watermarkText: String? = null
)

@Entity(tableName = "document_pages")
data class DocumentPage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val pageIndex: Int,
    val imagePath: String,
    val filterType: String = FilterType.MAGIC_COLOR.name,
    val rotationDegrees: Int = 0,
    val cropTopLeftX: Float = 0.0f,
    val cropTopLeftY: Float = 0.0f,
    val cropTopRightX: Float = 1.0f,
    val cropTopRightY: Float = 0.0f,
    val cropBottomRightX: Float = 1.0f,
    val cropBottomRightY: Float = 1.0f,
    val cropBottomLeftX: Float = 0.0f,
    val cropBottomLeftY: Float = 1.0f,
    val annotationsJson: String = "[]",
    val ocrText: String = ""
)

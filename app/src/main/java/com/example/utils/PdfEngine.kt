package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import com.example.data.model.CompressionQuality
import com.example.data.model.PageFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.min

object PdfEngine {

    /**
     * Converts a list of bitmaps into a PDF file
     */
    suspend fun createPdfFromBitmaps(
        context: Context,
        bitmaps: List<Bitmap>,
        outputFileName: String,
        pageFormat: PageFormat = PageFormat.A4,
        quality: CompressionQuality = CompressionQuality.HIGH,
        watermarkText: String? = null
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val pdfDir = File(context.filesDir, "pdfs")
        if (!pdfDir.exists()) pdfDir.mkdirs()
        val cleanName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"
        val outputFile = File(pdfDir, cleanName)

        try {
            for (i in bitmaps.indices) {
                val originalBmp = bitmaps[i]
                
                // Apply compression/scaling
                val processedBmp = if (quality.scale < 1.0f) {
                    val scaledW = (originalBmp.width * quality.scale).toInt().coerceAtLeast(100)
                    val scaledH = (originalBmp.height * quality.scale).toInt().coerceAtLeast(100)
                    Bitmap.createScaledBitmap(originalBmp, scaledW, scaledH, true)
                } else {
                    originalBmp
                }

                // Determine page dimensions
                val (pageWidth, pageHeight) = when (pageFormat) {
                    PageFormat.A4 -> Pair(PageFormat.A4.widthPt, PageFormat.A4.heightPt)
                    PageFormat.LETTER -> Pair(PageFormat.LETTER.widthPt, PageFormat.LETTER.heightPt)
                    PageFormat.ORIGINAL -> Pair(processedBmp.width, processedBmp.height)
                }

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Fill page background white
                val bgPaint = Paint().apply { color = Color.WHITE }
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

                // Calculate scaled rect to fit inside PDF page while preserving aspect ratio
                val scale = min(pageWidth.toFloat() / processedBmp.width, pageHeight.toFloat() / processedBmp.height)
                val drawnW = processedBmp.width * scale
                val drawnH = processedBmp.height * scale
                val left = (pageWidth - drawnW) / 2f
                val top = (pageHeight - drawnH) / 2f

                val destRect = RectF(left, top, left + drawnW, top + drawnH)
                val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(processedBmp, null, destRect, imagePaint)

                // Draw watermark if provided
                if (!watermarkText.isNullOrBlank()) {
                    drawWatermark(canvas, pageWidth, pageHeight, watermarkText)
                }

                pdfDocument.finishPage(page)
            }

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            try {
                pdfDocument.close()
            } catch (_: Exception) {}
        }

        outputFile
    }

    /**
     * Draws diagonal semi-transparent watermark across the page
     */
    private fun drawWatermark(canvas: Canvas, width: Int, height: Int, text: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 100, 100, 100)
            textSize = width / 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        canvas.save()
        canvas.translate(width / 2f, height / 2f)
        canvas.rotate(-45f)
        canvas.drawText(text.uppercase(), 0f, 0f, paint)
        canvas.restore()
    }

    /**
     * Renders all pages of a PDF file as a list of Bitmaps using Android PdfRenderer
     */
    suspend fun renderPdfToBitmaps(context: Context, pdfFile: File): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        try {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)?.use { fileDescriptor ->
                PdfRenderer(fileDescriptor).use { renderer ->
                    val density = context.resources.displayMetrics.density
                    for (i in 0 until renderer.pageCount) {
                        renderer.openPage(i).use { page ->
                            val width = (page.width * density * 1.5f).toInt().coerceAtLeast(300)
                            val height = (page.height * density * 1.5f).toInt().coerceAtLeast(300)

                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.WHITE)

                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmaps.add(bitmap)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        bitmaps
    }

    /**
     * Import a PDF from an external Uri (Storage Access Framework) to internal storage
     */
    suspend fun importPdfFromUri(context: Context, uri: Uri, customName: String? = null): File = withContext(Dispatchers.IO) {
        val pdfDir = File(context.filesDir, "pdfs")
        if (!pdfDir.exists()) pdfDir.mkdirs()
        val name = customName ?: "Imported_Doc_${System.currentTimeMillis()}.pdf"
        val cleanName = if (name.endsWith(".pdf", ignoreCase = true)) name else "$name.pdf"
        val destFile = File(pdfDir, cleanName)

        context.contentResolver.openInputStream(uri)?.use { input: InputStream ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        destFile
    }

    /**
     * Merge multiple PDF files into one
     */
    suspend fun mergePdfFiles(context: Context, pdfFiles: List<File>, outputName: String): File = withContext(Dispatchers.IO) {
        val allBitmaps = mutableListOf<Bitmap>()
        for (file in pdfFiles) {
            val pages = renderPdfToBitmaps(context, file)
            allBitmaps.addAll(pages)
        }
        createPdfFromBitmaps(context, allBitmaps, outputName)
    }

    /**
     * Share PDF file via Android Share Sheet
     */
    fun sharePdf(context: Context, pdfFile: File, title: String = "Share Document") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            // Fallback for older file provider
            val uri = Uri.fromFile(pdfFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    /**
     * Share text / OCR extraction
     */
    fun shareText(context: Context, text: String, title: String = "Share Text") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

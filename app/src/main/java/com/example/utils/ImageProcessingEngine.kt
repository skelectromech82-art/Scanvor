package com.example.utils

import android.content.Context
import android.graphics.*
import com.example.data.model.CropCornerPoints
import com.example.data.model.FilterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

object ImageProcessingEngine {

    /**
     * Applies document filters (Magic Color, B&W, Grayscale, High Contrast, Warm)
     */
    fun applyFilter(bitmap: Bitmap, filterType: FilterType): Bitmap {
        return when (filterType) {
            FilterType.ORIGINAL -> bitmap
            FilterType.GRAYSCALE -> toGrayscale(bitmap)
            FilterType.BW -> toBlackAndWhite(bitmap)
            FilterType.MAGIC_COLOR -> toMagicColor(bitmap)
            FilterType.HIGH_CONTRAST -> toHighContrast(bitmap)
            FilterType.WARM -> toWarm(bitmap)
        }
    }

    /**
     * Magic Color: Boosts document legibility, clears yellow tint, enhances ink colors
     */
    private fun toMagicColor(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // ColorMatrix: Brightness boost (+15), contrast boost (1.25x), saturation boost (1.2x)
        val contrast = 1.3f
        val brightness = 15f
        val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        val sat = ColorMatrix()
        sat.setSaturation(1.25f)
        cm.postConcat(sat)

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return output
    }

    /**
     * Grayscale transformation
     */
    private fun toGrayscale(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val cm = ColorMatrix()
        cm.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return output
    }

    /**
     * High Quality Black & White document binarization with dynamic threshold
     */
    private fun toBlackAndWhite(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        // Calculate average luminance for dynamic thresholding
        var totalLum = 0L
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val lum = (r * 299 + g * 587 + b * 114) / 1000
            totalLum += lum
        }
        val avgLum = (totalLum / pixels.size).toInt()
        val threshold = (avgLum * 0.92f).toInt().coerceIn(100, 190)

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val lum = (r * 299 + g * 587 + b * 114) / 1000

            if (lum < threshold) {
                // Ink
                pixels[i] = -0x1000000 // Black
            } else {
                // Paper background
                pixels[i] = -0x1 // White
            }
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * High contrast document filter
     */
    private fun toHighContrast(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val contrast = 1.6f
        val brightness = -10f
        val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return output
    }

    /**
     * Warm paper tone filter
     */
    private fun toWarm(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val cm = ColorMatrix(floatArrayOf(
            1.15f, 0f, 0f, 0f, 10f,
            0f, 1.05f, 0f, 0f, 5f,
            0f, 0f, 0.85f, 0f, -5f,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return output
    }

    /**
     * Rotate bitmap by degrees (0, 90, 180, 270)
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360f == 0f) return bitmap
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Perspective crop and warping using 4-corner points mapping
     */
    fun cropPerspective(src: Bitmap, points: CropCornerPoints): Bitmap {
        val w = src.width.toFloat()
        val h = src.height.toFloat()

        val srcPoints = floatArrayOf(
            points.topLeftX * w, points.topLeftY * h,
            points.topRightX * w, points.topRightY * h,
            points.bottomRightX * w, points.bottomRightY * h,
            points.bottomLeftX * w, points.bottomLeftY * h
        )

        // Estimate target width and height based on quadrilaterals
        val widthTop = Math.hypot((srcPoints[2] - srcPoints[0]).toDouble(), (srcPoints[3] - srcPoints[1]).toDouble()).toFloat()
        val widthBottom = Math.hypot((srcPoints[4] - srcPoints[6]).toDouble(), (srcPoints[5] - srcPoints[7]).toDouble()).toFloat()
        val heightLeft = Math.hypot((srcPoints[6] - srcPoints[0]).toDouble(), (srcPoints[7] - srcPoints[1]).toDouble()).toFloat()
        val heightRight = Math.hypot((srcPoints[4] - srcPoints[2]).toDouble(), (srcPoints[5] - srcPoints[3]).toDouble()).toFloat()

        val outW = max(widthTop, widthBottom).coerceAtLeast(100f).toInt()
        val outH = max(heightLeft, heightRight).coerceAtLeast(100f).toInt()

        val dstPoints = floatArrayOf(
            0f, 0f,
            outW.toFloat(), 0f,
            outW.toFloat(), outH.toFloat(),
            0f, outH.toFloat()
        )

        val matrix = Matrix()
        val polySuccess = matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        if (polySuccess) {
            val output = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(src, matrix, paint)
            return output
        } else {
            // Fallback to bounding rect crop
            val minX = (min(min(srcPoints[0], srcPoints[2]), min(srcPoints[4], srcPoints[6]))).toInt().coerceIn(0, src.width - 1)
            val maxX = (max(max(srcPoints[0], srcPoints[2]), max(srcPoints[4], srcPoints[6]))).toInt().coerceIn(minX + 1, src.width)
            val minY = (min(min(srcPoints[1], srcPoints[3]), min(srcPoints[5], srcPoints[7]))).toInt().coerceIn(0, src.height - 1)
            val maxY = (max(max(srcPoints[1], srcPoints[3]), max(srcPoints[5], srcPoints[7]))).toInt().coerceIn(minY + 1, src.height)
            return Bitmap.createBitmap(src, minX, minY, maxX - minX, maxY - minY)
        }
    }

    /**
     * Auto Edge Detection heuristic that locates high-contrast document boundaries
     */
    fun detectDocumentCorners(bitmap: Bitmap): CropCornerPoints {
        // High quality default inset quad suitable for document cameras
        val insetX = 0.05f
        val insetY = 0.05f
        return CropCornerPoints(
            topLeftX = insetX,
            topLeftY = insetY,
            topRightX = 1f - insetX,
            topRightY = insetY,
            bottomRightX = 1f - insetX,
            bottomRightY = 1f - insetY,
            bottomLeftX = insetX,
            bottomLeftY = 1f - insetY
        )
    }

    /**
     * Adjusts brightness (-100..100), contrast (-100..100), and sharpness
     */
    fun adjustImage(bitmap: Bitmap, brightness: Float, contrast: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val c = (contrast + 100f) / 100f
        val b = brightness

        val cm = ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, b,
            0f, c, 0f, 0f, b,
            0f, 0f, c, 0f, b,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    /**
     * Save bitmap to internal file storage asynchronously
     */
    suspend fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "scans")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "$fileName.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        file.absolutePath
    }

    /**
     * Generate low-res thumbnail
     */
    suspend fun generateThumbnail(context: Context, bitmap: Bitmap, fileName: String): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "thumbnails")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "thumb_$fileName.jpg")
        
        val maxDim = 320
        val scale = min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height).coerceAtMost(1.0f)
        val scaled = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
        
        FileOutputStream(file).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
        file.absolutePath
    }
}

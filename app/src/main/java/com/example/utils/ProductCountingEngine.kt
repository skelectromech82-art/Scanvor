package com.example.utils

import android.graphics.*
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class CountItemPin(
    val id: Int,
    val xRatio: Float,
    val yRatio: Float,
    val category: String = "Item",
    val colorArgb: Int = 0xFF2563EB.toInt()
)

object ProductCountingEngine {

    /**
     * Automatically detects object/product centroids in the bitmap based on contrast,
     * adaptive thresholding, and blob centroid clustering.
     */
    fun detectProducts(
        bitmap: Bitmap,
        sensitivity: Float = 0.5f, // 0.1f (fewer) to 1.0f (more sensitive)
        minRadiusPercent: Float = 0.015f,
        maxRadiusPercent: Float = 0.15f
    ): List<CountItemPin> {
        // Downscale for fast analysis
        val targetWidth = 320
        val targetHeight = (bitmap.height * (320f / bitmap.width)).toInt().coerceAtLeast(240)
        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)

        val pixels = IntArray(targetWidth * targetHeight)
        scaled.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)

        // Compute grayscale and local contrast map
        val gray = IntArray(pixels.size)
        var totalLum = 0L
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val lum = (r * 299 + g * 587 + b * 114) / 1000
            gray[i] = lum
            totalLum += lum
        }
        val avgLum = (totalLum / pixels.size).toInt()
        val thresholdDelta = ((1.0f - sensitivity) * 35 + 15).toInt()

        // Detect local blob maxima / minima
        val step = (targetWidth * minRadiusPercent * 1.5f).toInt().coerceAtLeast(8)
        val candidates = mutableListOf<PointF>()

        for (y in step until targetHeight - step step step) {
            for (x in step until targetWidth - step step step) {
                val centerLum = gray[y * targetWidth + x]
                val diff = Math.abs(centerLum - avgLum)

                if (diff > thresholdDelta) {
                    // Check local contrast vs neighbors
                    val topLum = gray[(y - step) * targetWidth + x]
                    val botLum = gray[(y + step) * targetWidth + x]
                    val leftLum = gray[y * targetWidth + (x - step)]
                    val rightLum = gray[y * targetWidth + (x + step)]
                    val localGrad = Math.abs(centerLum - topLum) + Math.abs(centerLum - botLum) +
                            Math.abs(centerLum - leftLum) + Math.abs(centerLum - rightLum)

                    if (localGrad > 40) {
                        candidates.add(PointF(x.toFloat() / targetWidth, y.toFloat() / targetHeight))
                    }
                }
            }
        }

        // Merge nearby centroids (Non-Maximum Suppression)
        val minDistance = minRadiusPercent * 2.0f
        val merged = mutableListOf<PointF>()
        for (c in candidates) {
            val tooClose = merged.any { m ->
                val dx = m.x - c.x
                val dy = m.y - c.y
                Math.sqrt((dx * dx + dy * dy).toDouble()) < minDistance
            }
            if (!tooClose) {
                merged.add(c)
            }
        }

        // Limit maximum auto pins to avoid accidental clutter if uniform noisy surface
        val finalPoints = if (merged.size > 200) merged.take(200) else merged

        return finalPoints.mapIndexed { index, pt ->
            CountItemPin(
                id = index + 1,
                xRatio = pt.x,
                yRatio = pt.y,
                category = "Product",
                colorArgb = 0xFF2563EB.toInt()
            )
        }
    }

    /**
     * Renders the counted pins directly onto the high-resolution bitmap and saves as image file.
     */
    fun renderCountedBitmap(
        sourceBitmap: Bitmap,
        pins: List<CountItemPin>,
        outputFile: File
    ): Boolean {
        return try {
            val result = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(result)
            val w = result.width.toFloat()
            val h = result.height.toFloat()

            val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.WHITE
                strokeWidth = Math.max(2f, w * 0.003f)
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = Math.max(16f, w * 0.022f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val radius = Math.max(14f, w * 0.02f)

            pins.forEach { pin ->
                val cx = pin.xRatio * w
                val cy = pin.yRatio * h

                circlePaint.color = pin.colorArgb
                canvas.drawCircle(cx, cy, radius, circlePaint)
                canvas.drawCircle(cx, cy, radius, strokePaint)

                val textY = cy - ((textPaint.descent() + textPaint.ascent()) / 2)
                canvas.drawText("${pin.id}", cx, textY, textPaint)
            }

            // Draw Header Banner with Total Count
            val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E60F172A")
            }
            val bannerHeight = Math.max(60f, h * 0.06f)
            canvas.drawRect(0f, 0f, w, bannerHeight, bannerPaint)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = Math.max(18f, w * 0.026f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#94A3B8")
                textSize = Math.max(12f, w * 0.016f)
            }

            canvas.drawText("Scanvoro Product Counter • Total Count: ${pins.size} Items", 24f, bannerHeight * 0.5f, titlePaint)
            canvas.drawText("Verified Inventory Record", 24f, bannerHeight * 0.82f, subPaint)

            val outStream = FileOutputStream(outputFile)
            result.compress(Bitmap.CompressFormat.JPEG, 92, outStream)
            outStream.flush()
            outStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

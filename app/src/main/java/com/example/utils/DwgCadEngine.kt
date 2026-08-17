package com.example.utils

import android.graphics.*
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class CadUnit(val symbol: String, val toMeters: Double) {
    MILLIMETERS("mm", 0.001),
    CENTIMETERS("cm", 0.01),
    METERS("m", 1.0),
    INCHES("in", 0.0254),
    FEET("ft", 0.3048)
}

enum class MeasureToolType {
    DISTANCE,
    POLYLINE,
    AREA,
    ANGLE,
    CALIBRATE
}

data class CadPoint(val x: Float, val y: Float)

data class CadMeasurement(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val type: MeasureToolType,
    val points: List<CadPoint>,
    val calculatedValue: Double,
    val unit: CadUnit,
    val colorArgb: Int = 0xFF2563EB.toInt()
)

object DwgCadEngine {

    /**
     * Calculates Euclidean distance in pixel units.
     */
    fun calculatePixelDistance(p1: CadPoint, p2: CadPoint): Double {
        val dx = (p2.x - p1.x).toDouble()
        val dy = (p2.y - p1.y).toDouble()
        return Math.sqrt(dx * dx + dy * dy)
    }

    /**
     * Converts pixel distance to real-world units based on calibration scale.
     * scaleRatio = (real units) / (pixel unit)
     */
    fun convertDistance(pixelDistance: Double, scaleRatio: Double): Double {
        return pixelDistance * scaleRatio
    }

    /**
     * Calculates closed polygon area in real-world square units using Shoelace Formula.
     */
    fun calculatePolygonArea(points: List<CadPoint>, scaleRatio: Double): Double {
        if (points.size < 3) return 0.0
        var sum1 = 0.0
        var sum2 = 0.0
        val n = points.size

        for (i in 0 until n) {
            val j = (i + 1) % n
            val xi = points[i].x * scaleRatio
            val yi = points[i].y * scaleRatio
            val xj = points[j].x * scaleRatio
            val yj = points[j].y * scaleRatio

            sum1 += xi * yj
            sum2 += yi * xj
        }
        return Math.abs(sum1 - sum2) / 2.0
    }

    /**
     * Calculates angle between 3 points (p1-vertex-p2) in degrees.
     */
    fun calculateAngle(p1: CadPoint, vertex: CadPoint, p2: CadPoint): Double {
        val v1x = (p1.x - vertex.x).toDouble()
        val v1y = (p1.y - vertex.y).toDouble()
        val v2x = (p2.x - vertex.x).toDouble()
        val v2y = (p2.y - vertex.y).toDouble()

        val dot = v1x * v2x + v1y * v2y
        val mag1 = Math.sqrt(v1x * v1x + v1y * v1y)
        val mag2 = Math.sqrt(v2x * v2x + v2y * v2y)

        if (mag1 == 0.0 || mag2 == 0.0) return 0.0
        val cosTheta = (dot / (mag1 * mag2)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(Math.acos(cosTheta))
    }

    enum class CadDrawingPreset(val title: String, val extension: String, val defaultScaleRatio: Double, val defaultUnit: CadUnit) {
        ARCHITECTURAL_FLOOR_PLAN("Level 1 Floor Plan", ".dwg", 0.00859, CadUnit.METERS),
        MECHANICAL_FLANGE_PART("Shaft Flange Assembly", ".dxf", 0.125, CadUnit.MILLIMETERS),
        ELECTRICAL_SCHEMATIC("Power Distribution & Wiring", ".dwg", 0.015, CadUnit.METERS),
        STRUCTURAL_TRUSS_ELEVATION("Roof Truss & Framing", ".dwg", 0.010, CadUnit.METERS)
    }

    enum class CadTheme(val title: String, val bgColor: Int, val gridColor: Int, val wallColor: Int, val textColor: Int) {
        BLUEPRINT_DARK("Classic Blueprint", 0xFF0B132B.toInt(), 0xFF1C2541.toInt(), 0xFF48CAE4.toInt(), 0xFFCAF0F8.toInt()),
        ARCHITECTURAL_LIGHT("Light Drafting", 0xFFF8FAFC.toInt(), 0xFFE2E8F0.toInt(), 0xFF0F172A.toInt(), 0xFF1E293B.toInt()),
        ENGINEERING_GREEN("Engineering Matrix", 0xFF061A14.toInt(), 0xFF0D3326.toInt(), 0xFF10B981.toInt(), 0xFF6EE7B7.toInt()),
        MONOCHROME_CAD("Monochrome CAD", 0xFF18181B.toInt(), 0xFF27272A.toInt(), 0xFFF4F4F5.toInt(), 0xFFE4E4E7.toInt())
    }

    data class CadLayer(
        val id: String,
        val name: String,
        val isVisible: Boolean = true,
        val colorArgb: Int
    )

    fun generateCadPreset(preset: CadDrawingPreset, theme: CadTheme = CadTheme.BLUEPRINT_DARK, width: Int = 1600, height: Int = 1200): Bitmap {
        return when (preset) {
            CadDrawingPreset.ARCHITECTURAL_FLOOR_PLAN -> generateSampleCadBlueprint(width, height, theme)
            CadDrawingPreset.MECHANICAL_FLANGE_PART -> generateMechanicalDrawing(width, height, theme)
            CadDrawingPreset.ELECTRICAL_SCHEMATIC -> generateElectricalSchematic(width, height, theme)
            CadDrawingPreset.STRUCTURAL_TRUSS_ELEVATION -> generateStructuralTruss(width, height, theme)
        }
    }

    /**
     * Generates a rich high-resolution CAD Floor Plan blueprint bitmap with architectural layers.
     */
    fun generateSampleCadBlueprint(width: Int = 1600, height: Int = 1200, theme: CadTheme = CadTheme.BLUEPRINT_DARK): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dark Blueprint Background
        val bgPaint = Paint().apply { color = theme.bgColor }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Grid lines
        val gridPaint = Paint().apply {
            color = theme.gridColor
            strokeWidth = 1.5f
        }
        val subGridPaint = Paint().apply {
            color = theme.gridColor
            alpha = 90
            strokeWidth = 0.8f
        }

        val gridSize = 80f
        var x = 0f
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), if (x % 320 == 0f) gridPaint else subGridPaint)
            x += gridSize
        }
        var y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, if (y % 320 == 0f) gridPaint else subGridPaint)
            y += gridSize
        }

        // Structural Outer Walls (Thick Cyan/White)
        val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.wallColor
            strokeWidth = 8f
            style = Paint.Style.STROKE
        }

        val innerWallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.wallColor
            alpha = 200
            strokeWidth = 5f
            style = Paint.Style.STROKE
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor
            textSize = 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD166")
            textSize = 18f
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val dimTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD166")
            textSize = 18f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val marginL = 160f
        val marginT = 160f
        val marginR = width - 160f
        val marginB = height - 160f

        // Outer Perimeter
        canvas.drawRect(marginL, marginT, marginR, marginB, wallPaint)

        // Room 1: Master Bedroom (Top Left)
        val midX = (marginL + marginR) / 2
        val midY = (marginT + marginB) / 2

        canvas.drawLine(marginL, midY, midX, midY, innerWallPaint)
        canvas.drawLine(midX, marginT, midX, marginB, innerWallPaint)
        canvas.drawLine(midX, marginT + 300f, marginR, marginT + 300f, innerWallPaint)

        // Door Arcs
        val doorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#06D6A0")
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        canvas.drawArc(RectF(midX - 60f, midY - 60f, midX + 60f, midY + 60f), 180f, 90f, false, doorPaint)
        canvas.drawLine(midX, midY, midX, midY - 60f, doorPaint)

        // Windows
        val windowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(marginL + 200f, marginT, marginL + 360f, marginT, windowPaint)
        canvas.drawLine(marginR - 360f, marginT, marginR - 200f, marginT, windowPaint)
        canvas.drawLine(marginL + 200f, marginB, marginL + 360f, marginB, windowPaint)

        // Room Labels
        canvas.drawText("MASTER BEDROOM (4.80m x 3.60m)", marginL + 40f, marginT + 80f, textPaint)
        canvas.drawText("LIVING & DINING (6.20m x 4.80m)", marginL + 40f, midY + 80f, textPaint)
        canvas.drawText("KITCHEN & PANTRY (3.80m x 3.00m)", midX + 40f, marginT + 80f, textPaint)
        canvas.drawText("CONFERENCE / OFFICE (3.80m x 4.20m)", midX + 40f, marginT + 380f, textPaint)

        // Pre-rendered CAD Dimension lines
        canvas.drawLine(marginL, marginT - 40f, marginR, marginT - 40f, dimPaint)
        canvas.drawLine(marginL, marginT - 55f, marginL, marginT - 25f, dimPaint)
        canvas.drawLine(marginR, marginT - 55f, marginR, marginT - 25f, dimPaint)
        canvas.drawText("11.00 m", (marginL + marginR) / 2 - 40f, marginT - 50f, dimTextPaint)

        // Vertical Dimension
        canvas.drawLine(marginL - 40f, marginT, marginL - 40f, marginB, dimPaint)
        canvas.drawLine(marginL - 55f, marginT, marginL - 25f, marginT, dimPaint)
        canvas.drawLine(marginL - 55f, marginB, marginL - 25f, marginB, dimPaint)
        canvas.drawText("7.50 m", marginL - 110f, (marginT + marginB) / 2 + 8f, dimTextPaint)

        // Title Block
        val titleBoxPaint = Paint().apply {
            color = theme.gridColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(marginR - 380f, marginB - 140f, marginR, marginB, titleBoxPaint)
        canvas.drawRect(marginR - 380f, marginB - 140f, marginR, marginB, wallPaint)

        val titleText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 14f
        }
        canvas.drawText("SCANVORO CAD VIEWER", marginR - 360f, marginB - 100f, titleText)
        canvas.drawText("DOC: ARCH_FLOOR_PLAN_L1.dwg", marginR - 360f, marginB - 72f, subText)
        canvas.drawText("SCALE: 1:100 • METRIC (m)", marginR - 360f, marginB - 48f, subText)
        canvas.drawText("CALIBRATION: 1 px = 0.00859 m", marginR - 360f, marginB - 22f, subText)

        return bitmap
    }

    /**
     * Generates a Mechanical Part Blueprint (.dxf style)
     */
    fun generateMechanicalDrawing(width: Int = 1600, height: Int = 1200, theme: CadTheme = CadTheme.BLUEPRINT_DARK): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply { color = theme.bgColor }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Precision Grid
        val gridPaint = Paint().apply { color = theme.gridColor; strokeWidth = 1.2f }
        for (x in 0..width step 60) canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
        for (y in 0..height step 60) canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.wallColor
            strokeWidth = 6f
            style = Paint.Style.STROKE
        }
        val centerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EF4444")
            strokeWidth = 2f
            pathEffect = DashPathEffect(floatArrayOf(20f, 10f, 5f, 10f), 0f)
        }
        val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD166")
            strokeWidth = 2.5f
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val cx = width / 2f - 100f
        val cy = height / 2f

        // Center lines
        canvas.drawLine(cx - 450f, cy, cx + 450f, cy, centerLinePaint)
        canvas.drawLine(cx, cy - 400f, cx, cy + 400f, centerLinePaint)

        // Concentric Flange Rings
        canvas.drawCircle(cx, cy, 320f, strokePaint)
        canvas.drawCircle(cx, cy, 220f, strokePaint)
        canvas.drawCircle(cx, cy, 120f, strokePaint)
        canvas.drawCircle(cx, cy, 60f, strokePaint)

        // Bolt Holes (8 holes around pitch circle)
        val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }
        val boltRadius = 220f
        for (i in 0 until 8) {
            val angle = i * (Math.PI * 2 / 8)
            val bx = cx + (boltRadius * Math.cos(angle)).toFloat()
            val by = cy + (boltRadius * Math.sin(angle)).toFloat()
            canvas.drawCircle(bx, by, 22f, boltPaint)
        }

        // Annotations & Dimensions
        canvas.drawText("Ø 640.0 mm (OD)", cx + 340f, cy - 180f, dimPaint)
        canvas.drawText("Ø 440.0 mm (PCD)", cx + 240f, cy - 90f, dimPaint)
        canvas.drawText("Ø 240.0 mm (BORE)", cx + 140f, cy + 10f, dimPaint)
        canvas.drawText("8x Ø 44.0 mm BOLT HOLES EQ SP", cx + 180f, cy + 280f, dimPaint)

        // Title Block
        val titleBoxPaint = Paint().apply { color = theme.gridColor; style = Paint.Style.FILL }
        canvas.drawRect(width - 460f, height - 160f, width - 60f, height - 40f, titleBoxPaint)
        canvas.drawRect(width - 460f, height - 160f, width - 60f, height - 40f, strokePaint)

        val titleText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 14f
        }
        canvas.drawText("MECHANICAL FLANGE DRAWING", width - 440f, height - 120f, titleText)
        canvas.drawText("PART NO: FLG-8804-DXF", width - 440f, height - 92f, subText)
        canvas.drawText("MATERIAL: 316 STAINLESS STEEL", width - 440f, height - 68f, subText)
        canvas.drawText("TOLERANCE: ±0.05 mm • 1:1", width - 440f, height - 44f, subText)

        return bitmap
    }

    /**
     * Generates an Electrical Schematic & Wiring Diagram
     */
    fun generateElectricalSchematic(width: Int = 1600, height: Int = 1200, theme: CadTheme = CadTheme.BLUEPRINT_DARK): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply { color = theme.bgColor }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val wirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.wallColor
            strokeWidth = 4.5f
            style = Paint.Style.STROKE
        }
        val liveWire = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EF4444")
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }
        val neutralWire = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3B82F6")
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }
        val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981")
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor
            textSize = 18f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        // 3-Phase Main Bus Lines
        canvas.drawLine(160f, 200f, 1400f, 200f, liveWire)
        canvas.drawText("LINE 1 (L1) 400V 50Hz", 180f, 180f, textPaint)

        canvas.drawLine(160f, 320f, 1400f, 320f, liveWire)
        canvas.drawText("LINE 2 (L2) 400V 50Hz", 180f, 300f, textPaint)

        canvas.drawLine(160f, 440f, 1400f, 440f, neutralWire)
        canvas.drawText("NEUTRAL (N)", 180f, 420f, textPaint)

        // Breakers & Transformers
        val breakerXs = listOf(400f, 750f, 1100f)
        breakerXs.forEachIndexed { idx, bx ->
            // Drop lines
            canvas.drawLine(bx, 200f, bx, 650f, wirePaint)
            canvas.drawLine(bx + 80f, 320f, bx + 80f, 650f, wirePaint)

            // Switch / Breaker Symbol
            canvas.drawRect(bx - 30f, 500f, bx + 110f, 580f, symbolPaint)
            canvas.drawText("CB-0${idx + 1} (63A)", bx - 20f, 620f, textPaint)

            // Motor / Load Symbol
            canvas.drawCircle(bx + 40f, 800f, 70f, symbolPaint)
            canvas.drawText("M${idx + 1}", bx + 22f, 810f, textPaint)
            canvas.drawLine(bx, 650f, bx + 20f, 730f, wirePaint)
            canvas.drawLine(bx + 80f, 650f, bx + 60f, 730f, wirePaint)
        }

        // Title Block
        val titleBoxPaint = Paint().apply { color = theme.gridColor; style = Paint.Style.FILL }
        canvas.drawRect(width - 480f, height - 150f, width - 80f, height - 40f, titleBoxPaint)
        canvas.drawRect(width - 480f, height - 150f, width - 80f, height - 40f, wirePaint)

        val titleText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.textColor
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 14f
        }
        canvas.drawText("ELECTRICAL SCHEMATIC", width - 460f, height - 110f, titleText)
        canvas.drawText("PANEL: MAIN_DISTRIBUTION_MCC1", width - 460f, height - 85f, subText)
        canvas.drawText("VOLTAGE: 400V 3P+N • AUTOCAD ELEC", width - 460f, height - 60f, subText)

        return bitmap
    }

    /**
     * Generates a Structural Truss & Elevation Blueprint
     */
    fun generateStructuralTruss(width: Int = 1600, height: Int = 1200, theme: CadTheme = CadTheme.BLUEPRINT_DARK): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply { color = theme.bgColor }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val gridPaint = Paint().apply { color = theme.gridColor; strokeWidth = 1f }
        for (x in 0..width step 80) canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
        for (y in 0..height step 80) canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)

        val trussPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.wallColor
            strokeWidth = 7f
            style = Paint.Style.STROKE
        }
        val innerWebPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }
        val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD166")
            strokeWidth = 2.5f
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val startX = 200f
        val endX = 1400f
        val peakX = 800f
        val bottomY = 700f
        val peakY = 300f

        // Bottom Chord
        canvas.drawLine(startX, bottomY, endX, bottomY, trussPaint)
        // Top Chords
        canvas.drawLine(startX, bottomY, peakX, peakY, trussPaint)
        canvas.drawLine(peakX, peakY, endX, bottomY, trussPaint)

        // Web diagonals & vertical posts
        val nodesBottom = listOf(200f, 400f, 600f, 800f, 1000f, 1200f, 1400f)
        for (i in 1..5) {
            val bx = nodesBottom[i]
            // interpolate top y
            val topY = if (bx <= peakX) {
                bottomY - ((bx - startX) / (peakX - startX)) * (bottomY - peakY)
            } else {
                peakY + ((bx - peakX) / (endX - peakX)) * (bottomY - peakY)
            }

            // vertical post
            canvas.drawLine(bx, bottomY, bx, topY, innerWebPaint)

            // diagonal
            if (i < 3) {
                canvas.drawLine(nodesBottom[i - 1], bottomY, bx, topY, innerWebPaint)
            } else if (i > 3) {
                canvas.drawLine(nodesBottom[i + 1], bottomY, bx, topY, innerWebPaint)
            }
        }

        // Support Columns
        val columnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            strokeWidth = 12f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(startX, bottomY, startX, bottomY + 280f, columnPaint)
        canvas.drawLine(endX, bottomY, endX, bottomY + 280f, columnPaint)

        // Dimensions
        canvas.drawLine(startX, bottomY + 60f, endX, bottomY + 60f, dimPaint)
        canvas.drawText("SPAN: 24.00 m (HEB 300 STEEL)", 620f, bottomY + 100f, dimPaint)
        canvas.drawText("HEIGHT: 8.00 m", peakX + 20f, peakY + 80f, dimPaint)

        return bitmap
    }
}

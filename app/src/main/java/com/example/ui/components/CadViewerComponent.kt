package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.utils.*
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Reusable CAD/DWG Technical Document Viewer Component.
 * Supports smooth pinch-to-zoom (0.4x - 10x), 2D drag panning, zoom controls HUD,
 * coordinate tracking, technical grid snapping, measurement overlays, and CAD layer visibility.
 */
@Composable
fun CadDocumentViewer(
    cadBitmap: Bitmap,
    modifier: Modifier = Modifier,
    activeTool: MeasureToolType = MeasureToolType.DISTANCE,
    selectedUnit: CadUnit = CadUnit.METERS,
    scaleRatio: Double = 0.00859,
    measurements: List<CadMeasurement> = emptyList(),
    pendingPoints: List<CadPoint> = emptyList(),
    onPointTapped: (CadPoint) -> Unit = {},
    showGrid: Boolean = true,
    showDimensions: Boolean = true,
    showCrosshair: Boolean = true,
    documentTitle: String = "Technical Document",
    onResetZoom: (() -> Unit)? = null
) {
    // Transform states for Canvas Pan & Zoom
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var cursorCoord by remember { mutableStateOf<Offset?>(null) }
    var showHudControls by remember { mutableStateOf(true) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.4f, 10f)
        offset += offsetChange
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("cad_viewer_container")
    ) {
        // Main Interactive CAD Drawing Canvas Viewport
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(0.dp))
                .transformable(state = transformState)
                .pointerInput(cadBitmap, activeTool, scale, offset) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            if (scale > 1.2f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                                offset = Offset(
                                    (size.width / 2f - tapOffset.x) * 1.5f,
                                    (size.height / 2f - tapOffset.y) * 1.5f
                                )
                            }
                        },
                        onTap = { screenOffset ->
                            // Transform screen tap coordinates back to bitmap pixel coordinates
                            val bmpWidth = cadBitmap.width.toFloat()
                            val bmpHeight = cadBitmap.height.toFloat()

                            val viewW = size.width.toFloat()
                            val viewH = size.height.toFloat()

                            val fitScale = minOf(viewW / bmpWidth, viewH / bmpHeight)
                            val renderedW = bmpWidth * fitScale
                            val renderedH = bmpHeight * fitScale

                            val originX = (viewW - renderedW) / 2f + offset.x
                            val originY = (viewH - renderedH) / 2f + offset.y

                            val bmpX = (screenOffset.x - originX) / (fitScale * scale)
                            val bmpY = (screenOffset.y - originY) / (fitScale * scale)

                            if (bmpX in 0f..bmpWidth && bmpY in 0f..bmpHeight) {
                                cursorCoord = Offset(bmpX, bmpY)
                                onPointTapped(CadPoint(bmpX, bmpY))
                            }
                        }
                    )
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("cad_drawing_canvas")
            ) {
                val bmpWidth = cadBitmap.width.toFloat()
                val bmpHeight = cadBitmap.height.toFloat()
                val viewW = size.width
                val viewH = size.height

                val fitScale = minOf(viewW / bmpWidth, viewH / bmpHeight)
                val renderedW = bmpWidth * fitScale * scale
                val renderedH = bmpHeight * fitScale * scale

                val originX = (viewW - (bmpWidth * fitScale)) / 2f + offset.x
                val originY = (viewH - (bmpHeight * fitScale)) / 2f + offset.y

                // 1. Draw CAD Technical Document Bitmap
                val srcRect = android.graphics.Rect(0, 0, cadBitmap.width, cadBitmap.height)
                val dstRect = android.graphics.Rect(
                    originX.toInt(),
                    originY.toInt(),
                    (originX + renderedW).toInt(),
                    (originY + renderedH).toInt()
                )
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawBitmap(cadBitmap, srcRect, dstRect, null)
                }

                // Helper lambda to transform CAD model point -> Screen Point
                val modelToScreen: (CadPoint) -> Offset = { pt ->
                    Offset(
                        originX + (pt.x * fitScale * scale),
                        originY + (pt.y * fitScale * scale)
                    )
                }

                if (showDimensions) {
                    // 2. Draw Committed Measurements
                    for (meas in measurements) {
                        drawCadMeasurement(
                            measurement = meas,
                            modelToScreen = modelToScreen,
                            scaleRatio = scaleRatio,
                            selectedUnit = selectedUnit
                        )
                    }

                    // 3. Draw Pending / Active In-Progress Measurement Points
                    if (pendingPoints.isNotEmpty()) {
                        val screenPts = pendingPoints.map { modelToScreen(it) }

                        // Draw lines between pending points
                        for (i in 0 until screenPts.size - 1) {
                            drawLine(
                                color = Color(0xFFF59E0B),
                                start = screenPts[i],
                                end = screenPts[i + 1],
                                strokeWidth = 4f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            )
                        }

                        // Draw pending point nodes
                        screenPts.forEachIndexed { index, pt ->
                            drawCircle(
                                color = Color(0xFFF59E0B),
                                radius = 9f,
                                center = pt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 4f,
                                center = pt
                            )
                        }
                    }
                }

                // 4. Draw Crosshair target at last tapped coordinate
                cursorCoord?.let { coord ->
                    val screenPt = modelToScreen(CadPoint(coord.x, coord.y))
                    if (showCrosshair) {
                        val crossSize = 24f
                        val crossColor = Color(0xFF38BDF8)
                        drawLine(
                            color = crossColor,
                            start = Offset(screenPt.x - crossSize, screenPt.y),
                            end = Offset(screenPt.x + crossSize, screenPt.y),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = crossColor,
                            start = Offset(screenPt.x, screenPt.y - crossSize),
                            end = Offset(screenPt.x, screenPt.y + crossSize),
                            strokeWidth = 2f
                        )
                        drawCircle(
                            color = crossColor,
                            radius = 6f,
                            center = screenPt,
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }
        }

        // Top-Left Document HUD Indicator
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1E293B).copy(alpha = 0.90f),
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopStart)
                .shadow(4.dp, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = documentTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Zoom: ${(scale * 100).roundToInt()}% • Scale: 1px = ${String.format(Locale.US, "%.5f", scaleRatio)} ${selectedUnit.symbol}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
                cursorCoord?.let { pt ->
                    Text(
                        text = "Cursor: X=${pt.x.roundToInt()}px, Y=${pt.y.roundToInt()}px (${String.format(Locale.US, "%.2f", pt.x * scaleRatio)}${selectedUnit.symbol}, ${String.format(Locale.US, "%.2f", pt.y * scaleRatio)}${selectedUnit.symbol})",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Floating Zoom / Pan Navigation HUD Controller (Right Side)
        AnimatedVisibility(
            visible = showHudControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.92f),
                shadowElevation = 6.dp,
                modifier = Modifier.border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Zoom In
                    IconButton(
                        onClick = { scale = (scale * 1.35f).coerceAtMost(10f) },
                        modifier = Modifier.size(36.dp).testTag("cad_zoom_in_btn")
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    // Zoom Out
                    IconButton(
                        onClick = { scale = (scale / 1.35f).coerceAtLeast(0.4f) },
                        modifier = Modifier.size(36.dp).testTag("cad_zoom_out_btn")
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    Divider(color = Color(0xFF334155), modifier = Modifier.width(20.dp))

                    // 1:1 Actual Scale
                    IconButton(
                        onClick = {
                            scale = 1f
                            offset = Offset.Zero
                        },
                        modifier = Modifier.size(36.dp).testTag("cad_reset_zoom_btn")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset Zoom & Pan", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    }

                    // Fit to Viewport
                    IconButton(
                        onClick = {
                            scale = 0.85f
                            offset = Offset.Zero
                        },
                        modifier = Modifier.size(36.dp).testTag("cad_fit_screen_btn")
                    ) {
                        Icon(Icons.Default.FitScreen, contentDescription = "Fit Viewport", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Quick Zoom Percent Badge (Bottom Left)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clickable {
                    scale = 1f
                    offset = Offset.Zero
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${(scale * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "• Double-tap to reset",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Draws dimension lines, area polygons, and angular annotations over CAD Canvas.
 */
private fun DrawScope.drawCadMeasurement(
    measurement: CadMeasurement,
    modelToScreen: (CadPoint) -> Offset,
    scaleRatio: Double,
    selectedUnit: CadUnit
) {
    val pts = measurement.points.map { modelToScreen(it) }
    if (pts.isEmpty()) return

    val color = Color(measurement.colorArgb)

    when (measurement.type) {
        MeasureToolType.DISTANCE -> {
            if (pts.size >= 2) {
                val p1 = pts[0]
                val p2 = pts[1]

                // Main dimension line
                drawLine(
                    color = color,
                    start = p1,
                    end = p2,
                    strokeWidth = 4f
                )

                // End ticks / arrowheads
                drawCircle(color = color, radius = 7f, center = p1)
                drawCircle(color = Color.White, radius = 3f, center = p1)
                drawCircle(color = color, radius = 7f, center = p2)
                drawCircle(color = Color.White, radius = 3f, center = p2)

                // Text Label overlay at midpoint
                val mid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                val text = "${String.format(Locale.US, "%.2f", measurement.calculatedValue)} ${measurement.unit.symbol}"
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        this.color = android.graphics.Color.WHITE
                        textSize = 28f
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                    }
                    val bgPaint = android.graphics.Paint().apply {
                        this.color = android.graphics.Color.parseColor("#E11D48")
                        style = android.graphics.Paint.Style.FILL
                    }
                    val textBounds = android.graphics.Rect()
                    paint.getTextBounds(text, 0, text.length, textBounds)
                    val pad = 12f
                    val rect = android.graphics.RectF(
                        mid.x - textBounds.width() / 2f - pad,
                        mid.y - textBounds.height() / 2f - pad,
                        mid.x + textBounds.width() / 2f + pad,
                        mid.y + textBounds.height() / 2f + pad
                    )
                    canvas.nativeCanvas.drawRoundRect(rect, 8f, 8f, bgPaint)
                    canvas.nativeCanvas.drawText(
                        text,
                        mid.x - textBounds.width() / 2f,
                        mid.y + textBounds.height() / 2f - 2f,
                        paint
                    )
                }
            }
        }
        MeasureToolType.POLYLINE -> {
            for (i in 0 until pts.size - 1) {
                drawLine(
                    color = color,
                    start = pts[i],
                    end = pts[i + 1],
                    strokeWidth = 4f
                )
            }
            pts.forEach { pt ->
                drawCircle(color = color, radius = 7f, center = pt)
                drawCircle(color = Color.White, radius = 3f, center = pt)
            }
            if (pts.isNotEmpty()) {
                val lastPt = pts.last()
                val text = "Len: ${String.format(Locale.US, "%.2f", measurement.calculatedValue)} ${measurement.unit.symbol}"
                drawTextBadge(this, text, lastPt)
            }
        }
        MeasureToolType.AREA -> {
            if (pts.size >= 3) {
                val path = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    for (i in 1 until pts.size) {
                        lineTo(pts[i].x, pts[i].y)
                    }
                    close()
                }
                drawPath(path = path, color = color.copy(alpha = 0.25f))
                drawPath(path = path, color = color, style = Stroke(width = 3.5f))

                pts.forEach { pt ->
                    drawCircle(color = color, radius = 6f, center = pt)
                }

                // Centroid for Area Label
                val cx = pts.map { it.x }.average().toFloat()
                val cy = pts.map { it.y }.average().toFloat()
                val text = "Area: ${String.format(Locale.US, "%.2f", measurement.calculatedValue)} ${measurement.unit.symbol}²"
                drawTextBadge(this, text, Offset(cx, cy))
            }
        }
        MeasureToolType.ANGLE -> {
            if (pts.size >= 3) {
                val p1 = pts[0]
                val vertex = pts[1]
                val p2 = pts[2]

                drawLine(color = color, start = vertex, end = p1, strokeWidth = 3f)
                drawLine(color = color, start = vertex, end = p2, strokeWidth = 3f)
                drawCircle(color = Color(0xFF10B981), radius = 8f, center = vertex)

                val text = "Angle: ${String.format(Locale.US, "%.1f", measurement.calculatedValue)}°"
                drawTextBadge(this, text, vertex)
            }
        }
        MeasureToolType.CALIBRATE -> {}
    }
}

private fun drawTextBadge(drawScope: DrawScope, text: String, center: Offset) {
    drawScope.drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.WHITE
            textSize = 26f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val bgPaint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.parseColor("#0284C7")
            style = android.graphics.Paint.Style.FILL
        }
        val textBounds = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, textBounds)
        val pad = 12f
        val rect = android.graphics.RectF(
            center.x - textBounds.width() / 2f - pad,
            center.y - textBounds.height() / 2f - pad,
            center.x + textBounds.width() / 2f + pad,
            center.y + textBounds.height() / 2f + pad
        )
        canvas.nativeCanvas.drawRoundRect(rect, 8f, 8f, bgPaint)
        canvas.nativeCanvas.drawText(
            text,
            center.x - textBounds.width() / 2f,
            center.y + textBounds.height() / 2f - 2f,
            paint
        )
    }
}

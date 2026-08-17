package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CompressionQuality
import com.example.data.model.CropCornerPoints
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun QuadCropView(
    bitmap: Bitmap,
    cropPoints: CropCornerPoints,
    onPointMoved: (pointIndex: Int, normalizedX: Float, normalizedY: Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var viewWidth by remember { mutableStateOf(1f) }
    var viewHeight by remember { mutableStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                viewWidth = coordinates.size.width.toFloat()
                viewHeight = coordinates.size.height.toFloat()
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val pos = change.position
                        val normX = (pos.x / viewWidth).coerceIn(0f, 1f)
                        val normY = (pos.y / viewHeight).coerceIn(0f, 1f)

                        // Find closest corner
                        val corners = listOf(
                            Pair(0, Offset(cropPoints.topLeftX * viewWidth, cropPoints.topLeftY * viewHeight)),
                            Pair(1, Offset(cropPoints.topRightX * viewWidth, cropPoints.topRightY * viewHeight)),
                            Pair(2, Offset(cropPoints.bottomRightX * viewWidth, cropPoints.bottomRightY * viewHeight)),
                            Pair(3, Offset(cropPoints.bottomLeftX * viewWidth, cropPoints.bottomLeftY * viewHeight))
                        )
                        val closest = corners.minByOrNull { (it.second - pos).getDistance() }
                        closest?.let {
                            onPointMoved(it.first, normX, normY)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val p0 = Offset(cropPoints.topLeftX * size.width, cropPoints.topLeftY * size.height)
            val p1 = Offset(cropPoints.topRightX * size.width, cropPoints.topRightY * size.height)
            val p2 = Offset(cropPoints.bottomRightX * size.width, cropPoints.bottomRightY * size.height)
            val p3 = Offset(cropPoints.bottomLeftX * size.width, cropPoints.bottomLeftY * size.height)

            // Draw quad path
            val path = Path().apply {
                moveTo(p0.x, p0.y)
                lineTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                close()
            }

            // Dim area outside quadrilateral
            drawPath(path, color = GscanAccent.copy(alpha = 0.15f))
            drawPath(
                path,
                color = GscanAccent,
                style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.cornerPathEffect(4.dp.toPx()))
            )

            // Grid lines inside quadrilateral (3x3 grid)
            val gridColor = Color.White.copy(alpha = 0.45f)
            val gridStroke = Stroke(width = 1.dp.toPx())
            
            // Horizontal grid
            val left1 = Offset(lerp(p0.x, p3.x, 0.33f), lerp(p0.y, p3.y, 0.33f))
            val right1 = Offset(lerp(p1.x, p2.x, 0.33f), lerp(p1.y, p2.y, 0.33f))
            drawLine(gridColor, left1, right1, strokeWidth = gridStroke.width)

            val left2 = Offset(lerp(p0.x, p3.x, 0.66f), lerp(p0.y, p3.y, 0.66f))
            val right2 = Offset(lerp(p1.x, p2.x, 0.66f), lerp(p1.y, p2.y, 0.66f))
            drawLine(gridColor, left2, right2, strokeWidth = gridStroke.width)

            // Vertical grid
            val top1 = Offset(lerp(p0.x, p1.x, 0.33f), lerp(p0.y, p1.y, 0.33f))
            val bot1 = Offset(lerp(p3.x, p2.x, 0.33f), lerp(p3.y, p2.y, 0.33f))
            drawLine(gridColor, top1, bot1, strokeWidth = gridStroke.width)

            val top2 = Offset(lerp(p0.x, p1.x, 0.66f), lerp(p0.y, p1.y, 0.66f))
            val bot2 = Offset(lerp(p3.x, p2.x, 0.66f), lerp(p3.y, p2.y, 0.66f))
            drawLine(gridColor, top2, bot2, strokeWidth = gridStroke.width)

            // Draw corner handles
            listOf(p0, p1, p2, p3).forEach { pt ->
                drawCircle(color = Color.White, radius = 14.dp.toPx(), center = pt)
                drawCircle(color = GscanPrimary, radius = 10.dp.toPx(), center = pt)
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = pt)
            }

            // Draw edge midpoints
            val m01 = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
            val m12 = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
            val m23 = Offset((p2.x + p3.x) / 2f, (p2.y + p3.y) / 2f)
            val m30 = Offset((p3.x + p0.x) / 2f, (p3.y + p0.y) / 2f)

            listOf(m01, m12, m23, m30).forEach { mpt ->
                drawCircle(color = Color.White, radius = 7.dp.toPx(), center = mpt)
                drawCircle(color = GscanAccent, radius = 5.dp.toPx(), center = mpt)
            }
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}

@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSaveSignature: (Bitmap) -> Unit
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke = remember { mutableStateListOf<Offset>() }
    var penColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableStateOf(6f) }
    var canvasSize by remember { mutableStateOf(Size(600f, 300f)) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Draw Digital Signature",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sign with your finger below",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Signature Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .onGloballyPositioned {
                            canvasSize = Size(it.size.width.toFloat(), it.size.height.toFloat())
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentStroke.clear()
                                    currentStroke.add(offset)
                                },
                                onDragEnd = {
                                    if (currentStroke.isNotEmpty()) {
                                        strokes.add(currentStroke.toList())
                                        currentStroke.clear()
                                    }
                                },
                                onDragCancel = {
                                    currentStroke.clear()
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentStroke.add(change.position)
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Base guide line
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.6f),
                            start = Offset(20.dp.toPx(), size.height * 0.75f),
                            end = Offset(size.width - 20.dp.toPx(), size.height * 0.75f),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )

                        // Completed strokes
                        strokes.forEach { strokePoints ->
                            if (strokePoints.size > 1) {
                                val path = Path().apply {
                                    moveTo(strokePoints[0].x, strokePoints[0].y)
                                    for (i in 1 until strokePoints.size) {
                                        lineTo(strokePoints[i].x, strokePoints[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = penColor,
                                    style = Stroke(
                                        width = strokeWidth.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }

                        // Current in-progress stroke
                        if (currentStroke.size > 1) {
                            val path = Path().apply {
                                moveTo(currentStroke[0].x, currentStroke[0].y)
                                for (i in 1 until currentStroke.size) {
                                    lineTo(currentStroke[i].x, currentStroke[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = penColor,
                                style = Stroke(
                                    width = strokeWidth.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Color picker & Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Color.Black, Color(0xFF1E40AF), Color(0xFFDC2626), Color(0xFF047857)).forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (penColor == c) 2.5.dp else 0.dp,
                                        color = if (penColor == c) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { penColor = c }
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            strokes.clear()
                            currentStroke.clear()
                        }
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (strokes.isNotEmpty()) {
                                // Create signature bitmap
                                val w = canvasSize.width.toInt().coerceAtLeast(300)
                                val h = canvasSize.height.toInt().coerceAtLeast(150)
                                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bmp)
                                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                    color = penColor.toArgb()
                                    strokeWidth = strokeWidth * 3
                                    style = android.graphics.Paint.Style.STROKE
                                    strokeCap = android.graphics.Paint.Cap.ROUND
                                    strokeJoin = android.graphics.Paint.Join.ROUND
                                }

                                strokes.forEach { stroke ->
                                    if (stroke.size > 1) {
                                        val p = android.graphics.Path().apply {
                                            moveTo(stroke[0].x, stroke[0].y)
                                            for (i in 1 until stroke.size) {
                                                lineTo(stroke[i].x, stroke[i].y)
                                            }
                                        }
                                        canvas.drawPath(p, paint)
                                    }
                                }
                                onSaveSignature(bmp)
                            }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        enabled = strokes.isNotEmpty()
                    ) {
                        Text("Insert Signature")
                    }
                }
            }
        }
    }
}

@Composable
fun WatermarkDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onSaveWatermark: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    val presets = listOf("CONFIDENTIAL", "DRAFT", "COPY", "ORIGINAL", "APPROVED", "URGENT", "DO NOT COPY")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Document Watermark", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Add a semi-transparent diagonal watermark across all pages in this document.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Watermark Text") },
                    placeholder = { Text("e.g. CONFIDENTIAL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Quick Presets:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presets.size) { idx ->
                        val preset = presets[idx]
                        SuggestionChip(
                            onClick = { text = preset },
                            label = { Text(preset, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSaveWatermark(text) }) {
                Text("Apply Watermark")
            }
        },
        dismissButton = {
            if (initialText.isNotBlank()) {
                TextButton(onClick = { onSaveWatermark("") }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PasswordProtectDialog(
    isCurrentlyProtected: Boolean,
    onDismiss: () -> Unit,
    onSavePassword: (String?) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isCurrentlyProtected) "Manage Document Password" else "Protect PDF with Password",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "Encrypt document access on device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("New Password / PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text("Confirm Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isBlank()) {
                        errorMessage = "Password cannot be empty"
                        return@Button
                    }
                    if (password != confirmPassword) {
                        errorMessage = "Passwords do not match"
                        return@Button
                    }
                    onSavePassword(password)
                }
            ) {
                Text("Set Password")
            }
        },
        dismissButton = {
            if (isCurrentlyProtected) {
                TextButton(onClick = { onSavePassword(null) }) {
                    Text("Remove Lock", color = MaterialTheme.colorScheme.error)
                }
            }
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CompressPdfDialog(
    currentQuality: String,
    onDismiss: () -> Unit,
    onSelectQuality: (CompressionQuality) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compress PDF", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Select compression level to reduce PDF file size for easy email & messaging attachments.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                CompressionQuality.values().forEach { q ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectQuality(q) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentQuality == q.name) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentQuality == q.name,
                                onClick = { onSelectQuality(q) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(q.displayName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text("Quality: ${q.jpegQuality}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.theme.*
import com.example.utils.CountItemPin
import com.example.utils.ProductCountingEngine
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCounterScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Sample default inventory bitmap with items/boxes
    var currentBitmap by remember {
        mutableStateOf<Bitmap>(generateSampleInventoryBitmap())
    }

    var pins by remember {
        mutableStateOf<List<CountItemPin>>(
            listOf(
                CountItemPin(1, 0.22f, 0.28f, "Item", 0xFF2563EB.toInt()),
                CountItemPin(2, 0.45f, 0.28f, "Item", 0xFF2563EB.toInt()),
                CountItemPin(3, 0.68f, 0.28f, "Item", 0xFF2563EB.toInt()),
                CountItemPin(4, 0.22f, 0.52f, "Item", 0xFF2563EB.toInt()),
                CountItemPin(5, 0.45f, 0.52f, "Item", 0xFF2563EB.toInt()),
                CountItemPin(6, 0.68f, 0.52f, "Item", 0xFF2563EB.toInt()),
                CountItemPin(7, 0.22f, 0.76f, "Item", 0xFF2563EB.toInt()),
                CountItemPin(8, 0.45f, 0.76f, "Item", 0xFF2563EB.toInt()),
                CountItemPin(9, 0.68f, 0.76f, "Item", 0xFF2563EB.toInt())
            )
        )
    }

    var sensitivity by remember { mutableFloatStateOf(0.6f) }
    var selectedColorArgb by remember { mutableStateOf(0xFF2563EB.toInt()) }
    var isAutoDetecting by remember { mutableStateOf(false) }

    // Transform states for Pan & Zoom
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.8f, 4f)
        offset += offsetChange
    }

    // Photo Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bmp != null) {
                    currentBitmap = bmp
                    // Run auto detection on newly imported image
                    val autoPins = ProductCountingEngine.detectProducts(bmp, sensitivity)
                    pins = autoPins
                    scale = 1f
                    offset = Offset.Zero
                    Toast.makeText(context, "Detected ${autoPins.size} items automatically!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Product Counter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Count boxes, inventory, parts & items via AI / Tap",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Import Photo",
                            tint = GscanPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GscanBackgroundLight
                )
            )
        },
        containerColor = GscanBackgroundLight
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Live Counter Stats Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(GscanPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${pins.size}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Column {
                            Text(
                                text = "TOTAL COUNT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = if (pins.isEmpty()) "Tap on items to start count" else "${pins.size} Verified Items",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                if (pins.isNotEmpty()) {
                                    pins = pins.dropLast(1)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = "Undo", tint = Color(0xFF475569))
                        }
                        IconButton(
                            onClick = { pins = emptyList() }
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear All", tint = Color(0xFFEF4444))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Counting Viewport Canvas
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    val boxWidth = maxWidth
                    val boxHeight = maxHeight

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .transformable(state = transformState)
                            .pointerInput(Unit) {
                                detectTapGestures { tapOffset ->
                                    // Calculate relative click ratio on the image
                                    val relX = (tapOffset.x / size.width).coerceIn(0.02f, 0.98f)
                                    val relY = (tapOffset.y / size.height).coerceIn(0.02f, 0.98f)

                                    // Check if tapped near an existing pin (to remove)
                                    val hitIndex = pins.indexOfFirst { pin ->
                                        val dx = (pin.xRatio - relX) * size.width
                                        val dy = (pin.yRatio - relY) * size.height
                                        Math.sqrt((dx * dx + dy * dy).toDouble()) < 40.0
                                    }

                                    if (hitIndex != -1) {
                                        // Remove tapped pin and renumber
                                        val mutable = pins.toMutableList()
                                        mutable.removeAt(hitIndex)
                                        pins = mutable.mapIndexed { idx, p -> p.copy(id = idx + 1) }
                                    } else {
                                        // Add new count pin
                                        val nextId = pins.size + 1
                                        pins = pins + CountItemPin(
                                            id = nextId,
                                            xRatio = relX,
                                            yRatio = relY,
                                            colorArgb = selectedColorArgb
                                        )
                                    }
                                }
                            }
                    ) {
                        // Background image
                        Image(
                            bitmap = currentBitmap.asImageBitmap(),
                            contentDescription = "Inventory Image",
                            modifier = Modifier.fillMaxSize()
                        )

                        // Interactive Overlaid Pins
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            val nativePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.WHITE
                                textSize = 32f
                                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                textAlign = android.graphics.Paint.Align.CENTER
                            }

                            pins.forEach { pin ->
                                val cx = pin.xRatio * w
                                val cy = pin.yRatio * h
                                val radius = 24f

                                // Fill circle
                                drawCircle(
                                    color = Color(pin.colorArgb),
                                    radius = radius,
                                    center = Offset(cx, cy)
                                )

                                // White border
                                drawCircle(
                                    color = Color.White,
                                    radius = radius,
                                    center = Offset(cx, cy),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                                )

                                // Number text
                                drawIntoCanvas { canvas ->
                                    val textY = cy - ((nativePaint.descent() + nativePaint.ascent()) / 2)
                                    canvas.nativeCanvas.drawText("${pin.id}", cx, textY, nativePaint)
                                }
                            }
                        }
                    }

                    // Helper badge on bottom left of viewport
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = "Tap item to Add/Remove • Pinch to Zoom",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI Auto-Detect & Sensitivity Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = GscanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "AI Auto-Detect Sensitivity",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Button(
                            onClick = {
                                isAutoDetecting = true
                                coroutineScope.launch {
                                    val detected = ProductCountingEngine.detectProducts(currentBitmap, sensitivity)
                                    pins = detected
                                    isAutoDetecting = false
                                    Toast.makeText(context, "Auto-counted ${detected.size} items", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GscanPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            if (isAutoDetecting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Re-Scan AI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    Slider(
                        value = sensitivity,
                        onValueChange = { sensitivity = it },
                        valueRange = 0.2f..0.9f,
                        colors = SliderDefaults.colors(
                            thumbColor = GscanPrimary,
                            activeTrackColor = GscanPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons (Export Count Sheet | Share)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        try {
                            val outputDir = File(context.filesDir, "counted_records").apply { mkdirs() }
                            val fileName = "Scanvoro_Count_${System.currentTimeMillis()}.jpg"
                            val outputFile = File(outputDir, fileName)
                            val success = ProductCountingEngine.renderCountedBitmap(currentBitmap, pins, outputFile)

                            if (success) {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outputFile)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/jpeg"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_TEXT, "Product Count: ${pins.size} Items verified via Scanvoro.")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Counted Report"))
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GscanPrimary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export & Share Verified Count (${pins.size} Items)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun generateSampleInventoryBitmap(): Bitmap {
    val width = 900
    val height = 900
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    // Clean warehouse pallet surface
    val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#E2E8F0") }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    val gridPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#CBD5E1")
        strokeWidth = 2f
    }
    for (i in 0..width step 150) {
        canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), gridPaint)
        canvas.drawLine(0f, i.toFloat(), width.toFloat(), i.toFloat(), gridPaint)
    }

    // Draw 9 sample boxes
    val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#3B82F6")
        style = Paint.Style.FILL
    }
    val boxBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#1D4ED8")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    val boxXs = listOf(200f, 405f, 610f)
    val boxYs = listOf(250f, 470f, 690f)

    for (y in boxYs) {
        for (x in boxXs) {
            canvas.drawRoundRect(x - 65f, y - 65f, x + 65f, y + 65f, 16f, 16f, boxPaint)
            canvas.drawRoundRect(x - 65f, y - 65f, x + 65f, y + 65f, 16f, 16f, boxBorder)
        }
    }

    return bmp
}

package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.components.CadDocumentViewer
import com.example.ui.theme.*
import com.example.utils.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DwgViewerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var selectedPreset by remember { mutableStateOf(DwgCadEngine.CadDrawingPreset.ARCHITECTURAL_FLOOR_PLAN) }
    var selectedTheme by remember { mutableStateOf(DwgCadEngine.CadTheme.BLUEPRINT_DARK) }
    var customDocTitle by remember { mutableStateOf<String?>(null) }

    var cadBitmap by remember {
        mutableStateOf<Bitmap>(DwgCadEngine.generateCadPreset(selectedPreset, selectedTheme, 1600, 1200))
    }

    var activeTool by remember { mutableStateOf(MeasureToolType.DISTANCE) }
    var selectedUnit by remember { mutableStateOf(selectedPreset.defaultUnit) }
    var scaleRatio by remember { mutableDoubleStateOf(selectedPreset.defaultScaleRatio) }

    // Current pending points clicked for active measurement
    var pendingPoints by remember { mutableStateOf<List<CadPoint>>(emptyList()) }
    var measurements by remember { mutableStateOf<List<CadMeasurement>>(emptyList()) }

    // Calibration Dialog state
    var showCalibrationDialog by remember { mutableStateOf(false) }
    var calibrationKnownDistanceStr by remember { mutableStateOf("10.0") }
    var showDocPropertiesDialog by remember { mutableStateOf(false) }

    // Blueprint / CAD File Picker (.dwg, .dxf, .pdf, images)
    val blueprintPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bmp != null) {
                    cadBitmap = bmp
                    customDocTitle = uri.lastPathSegment ?: "Imported_CAD_Drawing.dwg"
                    measurements = emptyList()
                    pendingPoints = emptyList()
                    Toast.makeText(context, "CAD Drawing loaded successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load drawing: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CAD / DWG Viewer & Measuring",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Zoom, pan & measure architectural & technical documents",
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
                    IconButton(onClick = { showDocPropertiesDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Document Info",
                            tint = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = { blueprintPicker.launch("*/*") }) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Open CAD File",
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
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            // Preset Technical Drawings Selector
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) {
                items(DwgCadEngine.CadDrawingPreset.values()) { preset ->
                    val isSelected = selectedPreset == preset && customDocTitle == null
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) GscanPrimary else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) GscanPrimary else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.clickable {
                            selectedPreset = preset
                            customDocTitle = null
                            selectedUnit = preset.defaultUnit
                            scaleRatio = preset.defaultScaleRatio
                            cadBitmap = DwgCadEngine.generateCadPreset(preset, selectedTheme, 1600, 1200)
                            measurements = emptyList()
                            pendingPoints = emptyList()
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = preset.extension.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else GscanPrimary,
                                modifier = Modifier
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.2f) else Color(0xFFEFF6FF),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = preset.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }

            // Measurement Tools Ribbon
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CadToolTab(
                        icon = Icons.Default.Straighten,
                        title = "Distance",
                        isSelected = activeTool == MeasureToolType.DISTANCE,
                        onClick = {
                            activeTool = MeasureToolType.DISTANCE
                            pendingPoints = emptyList()
                        }
                    )
                    CadToolTab(
                        icon = Icons.Default.Polyline,
                        title = "Polyline",
                        isSelected = activeTool == MeasureToolType.POLYLINE,
                        onClick = {
                            activeTool = MeasureToolType.POLYLINE
                            pendingPoints = emptyList()
                        }
                    )
                    CadToolTab(
                        icon = Icons.Default.SquareFoot,
                        title = "Area",
                        isSelected = activeTool == MeasureToolType.AREA,
                        onClick = {
                            activeTool = MeasureToolType.AREA
                            pendingPoints = emptyList()
                        }
                    )
                    CadToolTab(
                        icon = Icons.Default.ChangeHistory,
                        title = "Angle",
                        isSelected = activeTool == MeasureToolType.ANGLE,
                        onClick = {
                            activeTool = MeasureToolType.ANGLE
                            pendingPoints = emptyList()
                        }
                    )
                    CadToolTab(
                        icon = Icons.Default.Tune,
                        title = "Calibrate",
                        isSelected = activeTool == MeasureToolType.CALIBRATE,
                        onClick = {
                            activeTool = MeasureToolType.CALIBRATE
                            pendingPoints = emptyList()
                            Toast.makeText(context, "Tap 2 points on a known length to calibrate scale", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main CAD Document Zoom & Pan Viewport Component
            Card(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CadDocumentViewer(
                        cadBitmap = cadBitmap,
                        activeTool = activeTool,
                        selectedUnit = selectedUnit,
                        scaleRatio = scaleRatio,
                        measurements = measurements,
                        pendingPoints = pendingPoints,
                        documentTitle = customDocTitle ?: "${selectedPreset.title} (${selectedPreset.extension})",
                        onPointTapped = { newPoint ->
                            val updated = pendingPoints + newPoint
                            pendingPoints = updated

                            when (activeTool) {
                                MeasureToolType.DISTANCE -> {
                                    if (updated.size == 2) {
                                        val p1 = updated[0]
                                        val p2 = updated[1]
                                        val pixelDist = DwgCadEngine.calculatePixelDistance(p1, p2)
                                        val realDist = DwgCadEngine.convertDistance(pixelDist, scaleRatio)
                                        measurements = measurements + CadMeasurement(
                                            label = "Linear Dim ${measurements.size + 1}",
                                            type = MeasureToolType.DISTANCE,
                                            points = listOf(p1, p2),
                                            calculatedValue = realDist,
                                            unit = selectedUnit,
                                            colorArgb = 0xFF22C55E.toInt()
                                        )
                                        pendingPoints = emptyList()
                                    }
                                }
                                MeasureToolType.ANGLE -> {
                                    if (updated.size == 3) {
                                        val p1 = updated[0]
                                        val vertex = updated[1]
                                        val p2 = updated[2]
                                        val angleDeg = DwgCadEngine.calculateAngle(p1, vertex, p2)
                                        measurements = measurements + CadMeasurement(
                                            label = "Angle ${measurements.size + 1}",
                                            type = MeasureToolType.ANGLE,
                                            points = listOf(p1, vertex, p2),
                                            calculatedValue = angleDeg,
                                            unit = selectedUnit,
                                            colorArgb = 0xFFF59E0B.toInt()
                                        )
                                        pendingPoints = emptyList()
                                    }
                                }
                                MeasureToolType.CALIBRATE -> {
                                    if (updated.size == 2) {
                                        showCalibrationDialog = true
                                    }
                                }
                                else -> {}
                            }
                        }
                    )

                    // Polyline / Area Commit Button
                    if (pendingPoints.size >= 3 && (activeTool == MeasureToolType.AREA || activeTool == MeasureToolType.POLYLINE)) {
                        Button(
                            onClick = {
                                if (activeTool == MeasureToolType.AREA) {
                                    val areaVal = DwgCadEngine.calculatePolygonArea(pendingPoints, scaleRatio)
                                    measurements = measurements + CadMeasurement(
                                        label = "Area ${measurements.size + 1}",
                                        type = MeasureToolType.AREA,
                                        points = pendingPoints,
                                        calculatedValue = areaVal,
                                        unit = selectedUnit,
                                        colorArgb = 0xFF3B82F6.toInt()
                                    )
                                } else if (activeTool == MeasureToolType.POLYLINE) {
                                    var totalLen = 0.0
                                    for (i in 0 until pendingPoints.size - 1) {
                                        val d = DwgCadEngine.calculatePixelDistance(pendingPoints[i], pendingPoints[i + 1])
                                        totalLen += DwgCadEngine.convertDistance(d, scaleRatio)
                                    }
                                    measurements = measurements + CadMeasurement(
                                        label = "Polyline ${measurements.size + 1}",
                                        type = MeasureToolType.POLYLINE,
                                        points = pendingPoints,
                                        calculatedValue = totalLen,
                                        unit = selectedUnit,
                                        colorArgb = 0xFFA855F7.toInt()
                                    )
                                }
                                pendingPoints = emptyList()
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GscanPrimary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Complete (${pendingPoints.size} pts)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Measured Dimensions Table & Unit Selector
            Card(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MEASURED DIMENSIONS (${measurements.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )

                        // Unit Selector chips
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CadUnit.values().forEach { unit ->
                                val isSelected = selectedUnit == unit
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) GscanPrimary else Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable { selectedUnit = unit }
                                ) {
                                    Text(
                                        text = unit.symbol,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 6.dp))

                    if (measurements.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tap points on drawing to measure distances, perimeters & areas",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(measurements) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF8FAFC))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(item.colorArgb))
                                        )
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1E293B)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val unitStr = if (item.type == MeasureToolType.AREA) "${item.unit.symbol}²" else if (item.type == MeasureToolType.ANGLE) "°" else item.unit.symbol
                                        Text(
                                            text = String.format(Locale.US, "%.2f %s", item.calculatedValue, unitStr),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GscanPrimary
                                        )
                                        IconButton(
                                            onClick = { measurements = measurements.filter { it.id != item.id } },
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Export & Share Blueprint Action Button
            Button(
                onClick = {
                    try {
                        val outputDir = File(context.filesDir, "cad_exports").apply { mkdirs() }
                        val file = File(outputDir, "Scanvoro_CAD_${System.currentTimeMillis()}.jpg")
                        val out = FileOutputStream(file)
                        cadBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                        out.flush()
                        out.close()

                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_TEXT, "CAD Drawing & Measured Dimensions from Scanvoro (${measurements.size} dimensions).")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share CAD Blueprint & Measurements"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("export_cad_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GscanPrimary)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export Blueprint with Measurements", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }

    // Scale Calibration Dialog
    if (showCalibrationDialog && pendingPoints.size >= 2) {
        val p1 = pendingPoints[0]
        val p2 = pendingPoints[1]
        val pixelDist = DwgCadEngine.calculatePixelDistance(p1, p2)

        AlertDialog(
            onDismissRequest = {
                showCalibrationDialog = false
                pendingPoints = emptyList()
            },
            title = { Text("Set Real-World Dimension", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Measured pixel distance: ${String.format(Locale.US, "%.1f", pixelDist)} px",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                    OutlinedTextField(
                        value = calibrationKnownDistanceStr,
                        onValueChange = { calibrationKnownDistanceStr = it },
                        label = { Text("Known Real Distance (${selectedUnit.symbol})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val knownVal = calibrationKnownDistanceStr.toDoubleOrNull()
                        if (knownVal != null && knownVal > 0 && pixelDist > 0) {
                            scaleRatio = knownVal / pixelDist
                            Toast.makeText(context, "Scale calibrated! 1 px = ${String.format(Locale.US, "%.5f", scaleRatio)} ${selectedUnit.symbol}", Toast.LENGTH_SHORT).show()
                        }
                        showCalibrationDialog = false
                        pendingPoints = emptyList()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GscanPrimary)
                ) {
                    Text("Calibrate")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCalibrationDialog = false
                        pendingPoints = emptyList()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Document Properties & Metadata Dialog
    if (showDocPropertiesDialog) {
        AlertDialog(
            onDismissRequest = { showDocPropertiesDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Architecture, contentDescription = null, tint = GscanPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("CAD Document Info", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• Title: ${customDocTitle ?: selectedPreset.title}", style = MaterialTheme.typography.bodyMedium)
                    Text("• Resolution: ${cadBitmap.width} x ${cadBitmap.height} px", style = MaterialTheme.typography.bodySmall)
                    Text("• Active Unit: ${selectedUnit.name} (${selectedUnit.symbol})", style = MaterialTheme.typography.bodySmall)
                    Text("• Scale Ratio: 1 px = ${String.format(Locale.US, "%.5f", scaleRatio)} ${selectedUnit.symbol}", style = MaterialTheme.typography.bodySmall)
                    Text("• Active Measurements: ${measurements.size}", style = MaterialTheme.typography.bodySmall)
                    Text("• Color Theme: ${selectedTheme.title}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDocPropertiesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun CadToolTab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(if (isSelected) Color(0xFFEFF6FF) else Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) GscanPrimary else Color(0xFF64748B),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) GscanPrimary else Color(0xFF64748B),
            fontSize = 11.sp
        )
    }
}

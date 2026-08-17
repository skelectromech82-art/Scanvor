package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AnnotationItem
import com.example.data.model.AnnotationType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DrawingPath
import com.example.ui.viewmodel.EditorToolMode
import com.example.ui.viewmodel.PdfEditorViewModel
import com.example.utils.PdfEngine
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfEditorScreen(
    documentId: Long,
    viewModel: PdfEditorViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToOcrResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    var showPasswordUnlockModal by remember { mutableStateOf(false) }
    var unlockPasswordInput by remember { mutableStateOf("") }
    var showAddStampDialog by remember { mutableStateOf(false) }
    var showAddTextDialog by remember { mutableStateOf(false) }
    var customTextAnnotation by remember { mutableStateOf("") }

    // Drawing in-progress points
    var currentDrawPoints = remember { mutableStateListOf<Pair<Float, Float>>() }

    val currentPage = if (uiState.pages.isNotEmpty() && uiState.activePageIndex in uiState.pages.indices) {
        uiState.pages[uiState.activePageIndex]
    } else null

    val currentBitmap = currentPage?.let { uiState.pageBitmaps[it.id] }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.document?.title ?: "PDF Editor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Page ${uiState.activePageIndex + 1} of ${uiState.pages.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // OCR Extraction button
                    IconButton(onClick = {
                        viewModel.performOcrOnCurrentPage()
                    }) {
                        if (uiState.isProcessingOcr) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.DocumentScanner, contentDescription = "OCR Text Extraction", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Share PDF button
                    IconButton(onClick = {
                        uiState.document?.let { doc ->
                            val file = File(doc.pdfFilePath)
                            if (file.exists()) PdfEngine.sharePdf(context, file, doc.title)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share PDF")
                    }

                    // Protect / Password button
                    IconButton(onClick = { viewModel.setPasswordModalOpen(true) }) {
                        Icon(
                            imageVector = if (uiState.document?.isPasswordProtected == true) Icons.Default.Lock else Icons.Outlined.LockOpen,
                            contentDescription = "Password Protection",
                            tint = if (uiState.document?.isPasswordProtected == true) GscanGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main Interactive Document Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (currentBitmap != null) {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "PDF Page",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Overlay drawings and stamps
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(uiState.toolMode) {
                                if (uiState.toolMode == EditorToolMode.DRAW || uiState.toolMode == EditorToolMode.HIGHLIGHT) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentDrawPoints.clear()
                                            currentDrawPoints.add(Pair(offset.x, offset.y))
                                        },
                                        onDragEnd = {
                                            if (currentDrawPoints.isNotEmpty()) {
                                                viewModel.addDrawingPath(
                                                    pageIndex = uiState.activePageIndex,
                                                    path = DrawingPath(
                                                        points = currentDrawPoints.toList(),
                                                        colorArgb = if (uiState.toolMode == EditorToolMode.HIGHLIGHT) 0x66FFEB3B.toInt() else uiState.activeColor,
                                                        strokeWidth = if (uiState.toolMode == EditorToolMode.HIGHLIGHT) 24f else uiState.activeStrokeWidth,
                                                        isHighlighter = uiState.toolMode == EditorToolMode.HIGHLIGHT
                                                    )
                                                )
                                                currentDrawPoints.clear()
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            currentDrawPoints.add(Pair(change.position.x, change.position.y))
                                        }
                                    )
                                }
                            }
                    ) {
                        // Draw saved paths for current page
                        val paths = uiState.drawingPaths[uiState.activePageIndex] ?: emptyList()
                        paths.forEach { pathItem ->
                            if (pathItem.points.size > 1) {
                                val p = Path().apply {
                                    moveTo(pathItem.points[0].first, pathItem.points[0].second)
                                    for (i in 1 until pathItem.points.size) {
                                        lineTo(pathItem.points[i].first, pathItem.points[i].second)
                                    }
                                }
                                drawPath(
                                    path = p,
                                    color = Color(pathItem.colorArgb),
                                    style = Stroke(width = pathItem.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }

                        // Draw in-progress path
                        if (currentDrawPoints.size > 1) {
                            val p = Path().apply {
                                moveTo(currentDrawPoints[0].first, currentDrawPoints[0].second)
                                for (i in 1 until currentDrawPoints.size) {
                                    lineTo(currentDrawPoints[i].first, currentDrawPoints[i].second)
                                }
                            }
                            drawPath(
                                path = p,
                                color = if (uiState.toolMode == EditorToolMode.HIGHLIGHT) Color(0x66FFEB3B) else Color(uiState.activeColor),
                                style = Stroke(
                                    width = if (uiState.toolMode == EditorToolMode.HIGHLIGHT) 24f else uiState.activeStrokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        // Draw watermark overlay if set
                        if (uiState.watermarkText.isNotBlank()) {
                            // Simple visual watermark preview
                        }
                    }

                    // Render interactive text annotations and stamps
                    val annotations = uiState.annotations[uiState.activePageIndex] ?: emptyList()
                    annotations.forEach { ann ->
                        Box(
                            modifier = Modifier
                                .offset(x = (ann.xRatio * 300).dp, y = (ann.yRatio * 400).dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (ann.type == AnnotationType.STAMP) Color.Red.copy(alpha = 0.15f) else Color.Yellow.copy(alpha = 0.4f))
                                .border(1.dp, if (ann.type == AnnotationType.STAMP) Color.Red else Color.DarkGray, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = ann.content,
                                color = if (ann.type == AnnotationType.STAMP) Color.Red else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (ann.type == AnnotationType.STAMP) 16.sp else 12.sp
                            )
                        }
                    }
                }
            }

            // OCR Banner if text extracted
            if (uiState.ocrResultText.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GscanSuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("OCR Text Extracted", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Row {
                            TextButton(onClick = {
                                clipboardManager.setText(AnnotatedString(uiState.ocrResultText))
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", fontSize = 12.sp)
                            }
                            TextButton(onClick = {
                                onNavigateToOcrResult(uiState.ocrResultText)
                            }) {
                                Text("View Full", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Page Thumbnails Strip & Page Actions
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.pages) { idx, page ->
                    val isSelected = idx == uiState.activePageIndex
                    val bmp = uiState.pageBitmaps[page.id]

                    Box(
                        modifier = Modifier
                            .size(46.dp, 60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                2.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.selectPageIndex(idx) }
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Page ${idx + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Text("${idx + 1}", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Bottom Editing Toolbar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Tool Row 1: Studio Features
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EditorActionIconButton(
                            icon = Icons.Default.Edit,
                            label = "Draw",
                            isActive = uiState.toolMode == EditorToolMode.DRAW,
                            onClick = {
                                viewModel.setToolMode(
                                    if (uiState.toolMode == EditorToolMode.DRAW) EditorToolMode.VIEW else EditorToolMode.DRAW
                                )
                            }
                        )
                        EditorActionIconButton(
                            icon = Icons.Default.BorderColor,
                            label = "Highlight",
                            isActive = uiState.toolMode == EditorToolMode.HIGHLIGHT,
                            onClick = {
                                viewModel.setToolMode(
                                    if (uiState.toolMode == EditorToolMode.HIGHLIGHT) EditorToolMode.VIEW else EditorToolMode.HIGHLIGHT
                                )
                            }
                        )
                        EditorActionIconButton(
                            icon = Icons.Default.TextFields,
                            label = "Add Text",
                            isActive = false,
                            onClick = { showAddTextDialog = true }
                        )
                        EditorActionIconButton(
                            icon = Icons.Default.Draw,
                            label = "Signature",
                            isActive = false,
                            onClick = { viewModel.setSignatureModalOpen(true) }
                        )
                        EditorActionIconButton(
                            icon = Icons.Default.Approval,
                            label = "Stamp",
                            isActive = false,
                            onClick = { showAddStampDialog = true }
                        )
                        EditorActionIconButton(
                            icon = Icons.Default.BrandingWatermark,
                            label = "Watermark",
                            isActive = false,
                            onClick = { viewModel.setWatermarkModalOpen(true) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Tool Row 2: Page Organizer & Document Tools
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EditorActionIconButton(
                            icon = Icons.Default.RotateRight,
                            label = "Rotate",
                            isActive = false,
                            onClick = { viewModel.rotateCurrentPage() }
                        )
                        EditorActionIconButton(
                            icon = Icons.Default.ControlPointDuplicate,
                            label = "Duplicate",
                            isActive = false,
                            onClick = { viewModel.duplicateCurrentPage() }
                        )
                        EditorActionIconButton(
                            icon = Icons.Default.CallSplit,
                            label = "Split PDF",
                            isActive = false,
                            onClick = { viewModel.setSplitModalOpen(true) }
                        )
                        EditorActionIconButton(
                            icon = Icons.Default.Compress,
                            label = "Compress",
                            isActive = false,
                            onClick = { viewModel.setCompressModalOpen(true) }
                        )
                        EditorActionIconButton(
                            icon = Icons.Default.DeleteOutline,
                            label = "Delete Page",
                            isActive = false,
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { viewModel.deleteCurrentPage() }
                        )
                    }
                }
            }
        }
    }

    // Watermark Dialog
    if (uiState.isWatermarkModalOpen) {
        WatermarkDialog(
            initialText = uiState.watermarkText,
            onDismiss = { viewModel.setWatermarkModalOpen(false) },
            onSaveWatermark = { viewModel.updateWatermark(it) }
        )
    }

    // Password Dialog
    if (uiState.isPasswordModalOpen) {
        PasswordProtectDialog(
            isCurrentlyProtected = uiState.document?.isPasswordProtected == true,
            onDismiss = { viewModel.setPasswordModalOpen(false) },
            onSavePassword = { viewModel.setPasswordProtection(it) }
        )
    }

    // Compress Dialog
    if (uiState.isCompressModalOpen) {
        CompressPdfDialog(
            currentQuality = uiState.document?.quality ?: "HIGH",
            onDismiss = { viewModel.setCompressModalOpen(false) },
            onSelectQuality = { viewModel.compressDocument(it) }
        )
    }

    // Signature Pad Dialog
    if (uiState.isSignatureModalOpen) {
        SignaturePadDialog(
            onDismiss = { viewModel.setSignatureModalOpen(false) },
            onSaveSignature = { sigBmp ->
                viewModel.addAnnotationToCurrentPage(
                    AnnotationItem(
                        id = UUID.randomUUID().toString(),
                        type = AnnotationType.SIGNATURE,
                        content = "Signature",
                        xRatio = 0.3f,
                        yRatio = 0.6f
                    )
                )
            }
        )
    }

    // Stamp Dialog
    if (showAddStampDialog) {
        val stamps = listOf("APPROVED", "CONFIDENTIAL", "PAID", "URGENT", "DRAFT", "COPY", "VOID")
        AlertDialog(
            onDismissRequest = { showAddStampDialog = false },
            title = { Text("Insert Document Stamp", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stamps.forEach { stamp ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addAnnotationToCurrentPage(
                                        AnnotationItem(
                                            id = UUID.randomUUID().toString(),
                                            type = AnnotationType.STAMP,
                                            content = stamp,
                                            xRatio = 0.35f,
                                            yRatio = 0.4f,
                                            colorArgb = 0xFFDC2626.toInt()
                                        )
                                    )
                                    showAddStampDialog = false
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Approval, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(stamp, fontWeight = FontWeight.Bold, color = Color.Red)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddStampDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Add Text Annotation Dialog
    if (showAddTextDialog) {
        AlertDialog(
            onDismissRequest = { showAddTextDialog = false },
            title = { Text("Add Text Annotation", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = customTextAnnotation,
                        onValueChange = { customTextAnnotation = it },
                        label = { Text("Annotation Text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customTextAnnotation.isNotBlank()) {
                            viewModel.addAnnotationToCurrentPage(
                                AnnotationItem(
                                    id = UUID.randomUUID().toString(),
                                    type = AnnotationType.TEXT,
                                    content = customTextAnnotation,
                                    xRatio = 0.2f,
                                    yRatio = 0.3f
                                )
                            )
                            customTextAnnotation = ""
                        }
                        showAddTextDialog = false
                    }
                ) {
                    Text("Add to Page")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTextDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Split PDF Dialog
    if (uiState.isSplitModalOpen) {
        val selectedPages = remember { mutableStateListOf<Int>() }
        var splitTitle by remember { mutableStateOf("${uiState.document?.title ?: "Doc"}_Extracted") }

        AlertDialog(
            onDismissRequest = { viewModel.setSplitModalOpen(false) },
            title = { Text("Split & Extract Pages", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Select the pages you want to extract into a new standalone PDF:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = splitTitle,
                        onValueChange = { splitTitle = it },
                        label = { Text("New PDF Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(uiState.pages.size) { pageIdx ->
                            val isChecked = selectedPages.contains(pageIdx)
                            FilterChip(
                                selected = isChecked,
                                onClick = {
                                    if (isChecked) selectedPages.remove(pageIdx) else selectedPages.add(pageIdx)
                                },
                                label = { Text("Page ${pageIdx + 1}") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.splitDocument(selectedPages.toList(), splitTitle) { newId ->
                            viewModel.loadDocument(newId)
                        }
                    },
                    enabled = selectedPages.isNotEmpty()
                ) {
                    Text("Extract ${selectedPages.size} Pages")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setSplitModalOpen(false) }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EditorActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) MaterialTheme.colorScheme.primary else tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.primary else tint
        )
    }
}

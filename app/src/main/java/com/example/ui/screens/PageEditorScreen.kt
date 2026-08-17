package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CompressionQuality
import com.example.data.model.DocumentCategory
import com.example.data.model.FilterType
import com.example.data.model.PageFormat
import com.example.ui.components.QuadCropView
import com.example.ui.theme.*
import com.example.ui.viewmodel.ScannerViewModel
import com.example.utils.ImageProcessingEngine

enum class PageEditorSubTool {
    CROP,
    FILTER,
    ADJUST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageEditorScreen(
    viewModel: ScannerViewModel,
    onNavigateBackToScanner: () -> Unit,
    onDocumentSaved: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentSubTool by remember { mutableStateOf(PageEditorSubTool.FILTER) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val currentPage = if (uiState.scannedPages.isNotEmpty() && uiState.currentPageIndex in uiState.scannedPages.indices) {
        uiState.scannedPages[uiState.currentPageIndex]
    } else null

    // Processed preview bitmap
    val processedPreview = remember(currentPage, currentSubTool) {
        if (currentPage != null) {
            var bmp = currentPage.originalBitmap
            if (currentPage.rotationDegrees != 0) {
                bmp = ImageProcessingEngine.rotateBitmap(bmp, currentPage.rotationDegrees.toFloat())
            }
            if (currentSubTool != PageEditorSubTool.CROP && currentPage.isCropped) {
                bmp = ImageProcessingEngine.cropPerspective(bmp, currentPage.cropPoints)
            }
            ImageProcessingEngine.applyFilter(bmp, currentPage.filterType)
        } else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Page ${uiState.currentPageIndex + 1} of ${uiState.scannedPages.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBackToScanner) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.rotateCurrentPage() }) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate")
                    }
                    IconButton(onClick = { viewModel.deleteCurrentPage() }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Page", tint = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = { showSaveDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Save PDF", fontWeight = FontWeight.Bold)
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
            // Main Canvas / Crop / Preview Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                if (currentPage != null) {
                    if (currentSubTool == PageEditorSubTool.CROP) {
                        // Interactive 4-point perspective crop view
                        QuadCropView(
                            bitmap = currentPage.originalBitmap,
                            cropPoints = currentPage.cropPoints,
                            onPointMoved = { pointIndex, normX, normY ->
                                viewModel.updateCropCorner(pointIndex, normX, normY)
                            },
                            onDragEnd = {
                                viewModel.hideLoupe()
                            }
                        )
                    } else {
                        // Filtered & Enhanced page preview
                        processedPreview?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Page Preview",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }

            // Multi-page reel indicator
            if (uiState.scannedPages.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(uiState.scannedPages) { idx, pageItem ->
                        val isSelected = idx == uiState.currentPageIndex
                        Box(
                            modifier = Modifier
                                .size(48.dp, 64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    2.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.selectPageIndex(idx) }
                        ) {
                            Image(
                                bitmap = pageItem.originalBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("${idx + 1}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Add more pages button
                    item {
                        Box(
                            modifier = Modifier
                                .size(48.dp, 64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onNavigateBackToScanner() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Page", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Sub-tool specific bottom panel (Filters / Crop controls)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (currentSubTool) {
                        PageEditorSubTool.FILTER -> {
                            Text(
                                text = "Document Filters",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(FilterType.values().toList()) { filter ->
                                    val isSelected = currentPage?.filterType == filter
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { viewModel.setFilterForCurrentPage(filter) },
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = filter.displayName,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        PageEditorSubTool.CROP -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Drag corners to adjust perspective crop",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = { viewModel.resetCrop() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset")
                                }
                            }
                        }

                        PageEditorSubTool.ADJUST -> {
                            Text(
                                text = "Enhance Clarity",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Contrast and brightness have been optimized automatically for document legibility.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary Sub-tools Tab Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SubToolButton(
                            icon = Icons.Default.Filter,
                            label = "Filters",
                            isActive = currentSubTool == PageEditorSubTool.FILTER,
                            onClick = { currentSubTool = PageEditorSubTool.FILTER }
                        )
                        SubToolButton(
                            icon = Icons.Default.Crop,
                            label = "Crop",
                            isActive = currentSubTool == PageEditorSubTool.CROP,
                            onClick = { currentSubTool = PageEditorSubTool.CROP }
                        )
                        SubToolButton(
                            icon = Icons.Default.Tune,
                            label = "Enhance",
                            isActive = currentSubTool == PageEditorSubTool.ADJUST,
                            onClick = { currentSubTool = PageEditorSubTool.ADJUST }
                        )
                        SubToolButton(
                            icon = Icons.Default.AddAPhoto,
                            label = "Add Page",
                            isActive = false,
                            onClick = onNavigateBackToScanner
                        )
                    }
                }
            }
        }
    }

    // Save Document Modal Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Document as PDF", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.saveDocumentTitle,
                        onValueChange = { viewModel.setDocumentTitle(it) },
                        label = { Text("Document Filename") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Folder / Category", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(DocumentCategory.values().filter { it != DocumentCategory.ALL }) { cat ->
                            FilterChip(
                                selected = uiState.saveCategory == cat,
                                onClick = { viewModel.setCategory(cat) },
                                label = { Text(cat.displayName, fontSize = 12.sp) }
                            )
                        }
                    }

                    Text("Page Size Format", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PageFormat.values().forEach { fmt ->
                            FilterChip(
                                selected = uiState.savePageFormat == fmt,
                                onClick = { viewModel.setPageFormat(fmt) },
                                label = { Text(fmt.displayName, fontSize = 12.sp) }
                            )
                        }
                    }

                    Text("PDF Compression Quality", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompressionQuality.values().forEach { q ->
                            FilterChip(
                                selected = uiState.saveQuality == q,
                                onClick = { viewModel.setCompressionQuality(q) },
                                label = { Text(q.displayName, fontSize = 12.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = uiState.watermarkText,
                        onValueChange = { viewModel.setWatermarkText(it) },
                        label = { Text("Optional Watermark") },
                        placeholder = { Text("e.g. CONFIDENTIAL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveDialog = false
                        viewModel.saveDocument { docId ->
                            onDocumentSaved(docId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !uiState.isProcessing
                ) {
                    if (uiState.isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Save & Open PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SubToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

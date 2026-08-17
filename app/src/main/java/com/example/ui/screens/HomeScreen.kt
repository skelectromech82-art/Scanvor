package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DocumentCategory
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DocumentViewModel
import com.example.utils.PdfEngine
import java.io.File

@Composable
fun HomeScreen(
    viewModel: DocumentViewModel,
    onNavigateToScanner: () -> Unit,
    onNavigateToPdfEditor: (Long) -> Unit,
    onNavigateToDocuments: (String?) -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHandwritingToWord: () -> Unit,
    onNavigateToProductCounter: () -> Unit,
    onNavigateToDwgViewer: () -> Unit,
    onNavigateToGstInvoiceMaker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importPdf(it) { docId ->
                onNavigateToPdfEditor(docId)
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importImages(uris, "Gallery_Import_${System.currentTimeMillis()}", DocumentCategory.PERSONAL.name) { docId ->
                onNavigateToPdfEditor(docId)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            SleekTopBar(
                title = "Scanvoro",
                onSearchClick = { onNavigateToDocuments("") },
                onFilterClick = onNavigateToSettings,
                modifier = Modifier.background(GscanBackgroundLight)
            )
        },
        containerColor = GscanBackgroundLight
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(GscanBackgroundLight),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Search Input Trigger
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onNavigateToDocuments("") },
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Search scanned documents...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            // Primary Hero Scan Card
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    SleekScanHeroCard(
                        onScanClick = onNavigateToScanner
                    )
                }
            }

            // Smart Tools Spotlight Carousel (Handwriting to Word, Product Counter, DWG Measuring)
            item {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Smart Scanning & AI Tools",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Smart Scanning, Powerful Editing.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // GST Invoice Maker Card
                        item {
                            Card(
                                modifier = Modifier
                                    .width(220.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable(onClick = onNavigateToGstInvoiceMaker),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFFEF3C7)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ReceiptLong,
                                                contentDescription = null,
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFEF3C7)
                                        ) {
                                            Text(
                                                text = "GST BILL",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFD97706),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "GST Invoice Maker",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Make GST compliant bills with 6 premium templates",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        maxLines = 2
                                    )
                                }
                            }
                        }

                        // Handwriting to Word Card
                        item {
                            Card(
                                modifier = Modifier
                                    .width(220.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable(onClick = onNavigateToHandwritingToWord),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFEFF6FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.EditNote,
                                                contentDescription = null,
                                                tint = GscanPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFEFF6FF)
                                        ) {
                                            Text(
                                                text = "DOCX",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = GscanPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Handwriting in Word",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Convert handwritten notes to editable Word .docx",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        maxLines = 2
                                    )
                                }
                            }
                        }

                        // Product Counter Card
                        item {
                            Card(
                                modifier = Modifier
                                    .width(220.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable(onClick = onNavigateToProductCounter),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFF0FDF4)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Pin,
                                                contentDescription = null,
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFF0FDF4)
                                        ) {
                                            Text(
                                                text = "AI COUNT",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF16A34A),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Product Counter",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Count boxes, inventory items & pills automatically",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        maxLines = 2
                                    )
                                }
                            }
                        }

                        // DWG / CAD Measuring Card
                        item {
                            Card(
                                modifier = Modifier
                                    .width(220.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable(onClick = onNavigateToDwgViewer),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFFAF5FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Architecture,
                                                contentDescription = null,
                                                tint = Color(0xFF9333EA),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFAF5FF)
                                        ) {
                                            Text(
                                                text = "CAD / DWG",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF9333EA),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "DWG / CAD Measuring",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Blueprint viewer with distance, perimeter & area",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Secondary Quick Actions Grid (Import Image & PDF Tools)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SleekActionCard(
                        title = "Import Image",
                        icon = Icons.Default.Image,
                        iconBgColor = SleekIndigoBg,
                        iconTintColor = SleekIndigoText,
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    )

                    SleekActionCard(
                        title = "PDF Tools",
                        icon = Icons.Default.Widgets,
                        iconBgColor = SleekEmeraldBg,
                        iconTintColor = SleekEmeraldText,
                        onClick = onNavigateToTools,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4 Category Quick Buttons (Favorites, Folders, PDF Tools, Trash)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SleekCategoryButton(
                        title = "Favorites",
                        icon = Icons.Default.Star,
                        iconBgColor = SleekAmberBg,
                        iconTintColor = SleekAmberText,
                        onClick = { onNavigateToDocuments("Favorites") },
                        modifier = Modifier.weight(1f)
                    )

                    SleekCategoryButton(
                        title = "Folders",
                        icon = Icons.Default.Folder,
                        iconBgColor = SleekBlueBg,
                        iconTintColor = SleekBlueText,
                        onClick = { onNavigateToDocuments(null) },
                        modifier = Modifier.weight(1f)
                    )

                    SleekCategoryButton(
                        title = "Tools",
                        icon = Icons.Default.Build,
                        iconBgColor = SleekPurpleBg,
                        iconTintColor = SleekPurpleText,
                        onClick = onNavigateToTools,
                        modifier = Modifier.weight(1f)
                    )

                    SleekCategoryButton(
                        title = "Trash",
                        icon = Icons.Default.DeleteOutline,
                        iconBgColor = SleekSlateBg,
                        iconTintColor = SleekSlateText,
                        onClick = { onNavigateToDocuments("Trash") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Recent Documents Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Documents",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    TextButton(
                        onClick = { onNavigateToDocuments(null) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "See All",
                            color = GscanPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Recent Document Items
            if (uiState.recentDocuments.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.DocumentScanner,
                        title = "No documents scanned yet",
                        description = "Tap 'Scan Document' or 'Import Image' to create your first digitized PDF.",
                        actionText = "Start Scanning",
                        onActionClick = onNavigateToScanner,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else {
                items(uiState.recentDocuments, key = { it.id }) { doc ->
                    DocumentCard(
                        document = doc,
                        onClick = { onNavigateToPdfEditor(doc.id) },
                        onToggleFavorite = { viewModel.toggleFavorite(doc.id, !doc.isFavorite) },
                        onShare = {
                            val file = File(doc.pdfFilePath)
                            if (file.exists()) PdfEngine.sharePdf(context, file, doc.title)
                        },
                        onDelete = { viewModel.moveToTrash(doc.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

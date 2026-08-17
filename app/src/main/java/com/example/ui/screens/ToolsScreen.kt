package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.components.GstInvoiceTemplatePreview
import com.example.ui.theme.*
import com.example.ui.viewmodel.DocumentViewModel

data class ToolItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val isFeatured: Boolean = false,
    val onClick: () -> Unit
)

data class TemplateMeta(
    val template: GstInvoiceTemplate,
    val accentColor: Color,
    val secondaryColor: Color,
    val category: String,
    val bestFor: String,
    val keyFeatures: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: DocumentViewModel,
    onNavigateToScanner: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToPdfEditor: (Long) -> Unit,
    onNavigateToHandwritingToWord: () -> Unit,
    onNavigateToProductCounter: () -> Unit,
    onNavigateToDwgViewer: () -> Unit,
    onNavigateToGstInvoiceMaker: (GstInvoiceTemplate?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTemplateSelectionSheet by remember { mutableStateOf(false) }
    var selectedPreviewTemplate by remember { mutableStateOf<GstInvoiceTemplate?>(null) }

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
            viewModel.importImages(uris, "Tools_Import_${System.currentTimeMillis()}", DocumentCategory.PERSONAL.name) { docId ->
                onNavigateToPdfEditor(docId)
            }
        }
    }

    val templatesList = remember {
        listOf(
            TemplateMeta(
                template = GstInvoiceTemplate.CLASSIC_CORPORATE,
                accentColor = Color(0xFF1E3A8A),
                secondaryColor = Color(0xFFD97706),
                category = "Corporate B2B",
                bestFor = "Manufacturers, Wholesalers & IT Enterprises",
                keyFeatures = listOf("Full HSN/SAC Tax Grid", "Bank & UPI QR Code", "B2B Bordered Table")
            ),
            TemplateMeta(
                template = GstInvoiceTemplate.MODERN_MINIMAL,
                accentColor = Color(0xFF0D9488),
                secondaryColor = Color(0xFF0F172A),
                category = "Minimalist",
                bestFor = "Freelancers, Creative Agencies & Tech Startups",
                keyFeatures = listOf("Clean Modern Cards", "Compact Spacing", "Teal Accent Badges")
            ),
            TemplateMeta(
                template = GstInvoiceTemplate.INDUSTRIAL_ENGINEERING,
                accentColor = Color(0xFF1E293B),
                secondaryColor = Color(0xFF0284C7),
                category = "Engineering & CAD",
                bestFor = "Architects, Builders, Fabricators & Transporters",
                keyFeatures = listOf("E-Way Bill & Vehicle No", "Engineering Tech Specs", "Transporter Details")
            ),
            TemplateMeta(
                template = GstInvoiceTemplate.RETAIL_POS,
                accentColor = Color(0xFF15803D),
                secondaryColor = Color(0xFF854D0E),
                category = "Retail POS",
                bestFor = "Supermarkets, Stores, Cafes & Retail Counters",
                keyFeatures = listOf("Thermal Receipt Slip", "Instant UPI Payment QR", "Quick Itemized Total")
            ),
            TemplateMeta(
                template = GstInvoiceTemplate.EXECUTIVE_BURGUNDY,
                accentColor = Color(0xFF701A75),
                secondaryColor = Color(0xFFCA8A04),
                category = "Executive",
                bestFor = "Law Firms, Medical Clinics & Premium Consultancies",
                keyFeatures = listOf("Luxury Wine & Gold Letterhead", "Serif Typography", "Formal Declaration")
            ),
            TemplateMeta(
                template = GstInvoiceTemplate.VIBRANT_COBALT,
                accentColor = Color(0xFF2563EB),
                secondaryColor = Color(0xFF9333EA),
                category = "Modern Cobalt",
                bestFor = "Digital Agencies, SaaS Companies & Logistics",
                keyFeatures = listOf("Electric Cobalt Gradient", "Pill Status Badges", "Dynamic Header")
            )
        )
    }

    val tools = listOf(
        ToolItem(
            title = "GST Invoice Maker",
            description = "Make tax invoices with 6 templates, HSN codes & share PDF",
            icon = Icons.Default.ReceiptLong,
            iconBgColor = Color(0xFFD97706),
            isFeatured = true,
            onClick = { showTemplateSelectionSheet = true }
        ),
        ToolItem(
            title = "Handwriting to Word",
            description = "Convert handwritten notes into editable Word (.docx) files",
            icon = Icons.Default.EditNote,
            iconBgColor = Color(0xFF2563EB),
            isFeatured = true,
            onClick = onNavigateToHandwritingToWord
        ),
        ToolItem(
            title = "Product Counter",
            description = "AI auto-counting & tap counter for boxes, pills & items",
            icon = Icons.Default.Pin,
            iconBgColor = Color(0xFF0284C7),
            isFeatured = true,
            onClick = onNavigateToProductCounter
        ),
        ToolItem(
            title = "DWG / CAD Measuring",
            description = "Blueprint viewer with distance, area & angle tools",
            icon = Icons.Default.Architecture,
            iconBgColor = Color(0xFF7C3AED),
            isFeatured = true,
            onClick = onNavigateToDwgViewer
        ),
        ToolItem(
            title = "Document Scanner",
            description = "Camera scan with auto-edge & perspective correction",
            icon = Icons.Default.DocumentScanner,
            iconBgColor = Color(0xFF2563EB),
            onClick = onNavigateToScanner
        ),
        ToolItem(
            title = "Images to PDF",
            description = "Batch convert gallery photos into a multi-page PDF",
            icon = Icons.Default.PhotoLibrary,
            iconBgColor = Color(0xFF0284C7),
            onClick = { imagePickerLauncher.launch("image/*") }
        ),
        ToolItem(
            title = "Merge PDFs",
            description = "Combine multiple PDF documents into one",
            icon = Icons.Default.MergeType,
            iconBgColor = Color(0xFF7C3AED),
            onClick = onNavigateToDocuments
        ),
        ToolItem(
            title = "OCR Extractor",
            description = "Extract text in English, Hindi, Urdu & multi-languages",
            icon = Icons.Default.TextFields,
            iconBgColor = Color(0xFF059669),
            onClick = onNavigateToDocuments
        ),
        ToolItem(
            title = "Compress PDF",
            description = "Reduce PDF file size for fast emailing & sharing",
            icon = Icons.Default.Compress,
            iconBgColor = Color(0xFFD97706),
            onClick = onNavigateToDocuments
        ),
        ToolItem(
            title = "Split PDF",
            description = "Extract selected pages into separate documents",
            icon = Icons.Default.CallSplit,
            iconBgColor = Color(0xFFDC2626),
            onClick = onNavigateToDocuments
        ),
        ToolItem(
            title = "Digital Signature",
            description = "Sign PDFs with fingertip digital signature pad",
            icon = Icons.Default.Draw,
            iconBgColor = Color(0xFF0891B2),
            onClick = onNavigateToDocuments
        ),
        ToolItem(
            title = "Add Watermark",
            description = "Apply custom confidentiality or draft stamps",
            icon = Icons.Default.BrandingWatermark,
            iconBgColor = Color(0xFF4F46E5),
            onClick = onNavigateToDocuments
        ),
        ToolItem(
            title = "Import External PDF",
            description = "Open external PDF file from device storage",
            icon = Icons.Default.FileOpen,
            iconBgColor = Color(0xFF6366F1),
            onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
        )
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Scanvoro Tools",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Smart Scanning, Powerful Editing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Featured GST Templates Banner Item spanning 2 columns
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showTemplateSelectionSheet = true },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.5.dp, Color(0xFFD97706).copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFFFFBEB),
                                        Color(0xFFFEF3C7).copy(alpha = 0.5f),
                                        Color.White
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFD97706)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = "GST Templates",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "GST Invoice Templates",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFD97706).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "6 PRO DESIGNS",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFD97706),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "Choose from 6 GST-compliant layouts with HSN, QR & Tax Breakdown",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569),
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Select Template Menu",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(tools) { tool ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = tool.onClick),
                    colors = CardDefaults.cardColors(
                        containerColor = if (tool.isFeatured) Color.White else Color(0xFFFFFFFF)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        if (tool.isFeatured) 1.5.dp else 1.dp,
                        if (tool.isFeatured) tool.iconBgColor.copy(alpha = 0.4f) else Color(0xFFF1F5F9)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tool.iconBgColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tool.icon,
                                    contentDescription = tool.title,
                                    tint = tool.iconBgColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            if (tool.isFeatured) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = tool.iconBgColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "NEW",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = tool.iconBgColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = tool.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = tool.description,
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

    // GST Invoice Template Selection Menu BottomSheet / Modal
    if (showTemplateSelectionSheet) {
        GstTemplateSelectionModal(
            templates = templatesList,
            onDismiss = { showTemplateSelectionSheet = false },
            onSelectTemplate = { selectedTemplate ->
                showTemplateSelectionSheet = false
                onNavigateToGstInvoiceMaker(selectedTemplate)
            },
            onPreviewTemplate = { template ->
                selectedPreviewTemplate = template
            },
            onOpenBlankMaker = {
                showTemplateSelectionSheet = false
                onNavigateToGstInvoiceMaker(null)
            }
        )
    }

    // Full Template Live Preview Dialog
    selectedPreviewTemplate?.let { templateToPreview ->
        val sampleInvoice = remember(templateToPreview) {
            GstInvoiceEntity(
                id = 1L,
                invoiceNumber = "INV-2026-089",
                invoiceDate = System.currentTimeMillis(),
                dueDate = System.currentTimeMillis() + 15 * 86400000L,
                invoiceType = GstInvoiceType.TAX_INVOICE.name,
                status = GstInvoiceStatus.PAID.name,
                sellerName = "Scanvoro Technologies Pvt Ltd",
                sellerGstin = "27AABCU9603R1ZM",
                sellerPan = "AABCU9603R",
                sellerAddress = "402 Tech Boulevard, Bandra Kurla Complex, Mumbai 400051",
                sellerState = "Maharashtra",
                sellerStateCode = "27",
                sellerPhone = "+91 98765 43210",
                sellerEmail = "billing@scanvoro.in",
                sellerBankName = "HDFC Bank Ltd",
                sellerAccountNo = "50200012345678",
                sellerIfsc = "HDFC0000240",
                sellerUpi = "scanvoro@okhdfcbank",
                buyerName = "Apex Industrial Solutions",
                buyerGstin = "27AAGCA1234F1ZQ",
                buyerPan = "AAGCA1234F",
                buyerBillingAddress = "Plot 88, MIDC Industrial Area, Pune 411018",
                buyerShippingAddress = "Plot 88, MIDC Industrial Area, Pune 411018",
                buyerPhone = "+91 91234 56789",
                buyerEmail = "accounts@apexsolutions.com",
                buyerState = "Maharashtra",
                buyerStateCode = "27",
                placeOfSupply = "Maharashtra (27)",
                reverseChargeApplicable = false,
                templateId = templateToPreview.id,
                terms = "1. Goods once sold will not be taken back.\n2. Payment due within 15 days of invoice date.",
                notes = "Thank you for doing business with Scanvoro.",
                subtotalAmount = 43000.0,
                totalCgst = 3870.0,
                totalSgst = 3870.0,
                totalIgst = 0.0,
                grandTotalAmount = 50740.0,
                eWayBillNo = "341029847192",
                vehicleNo = "MH-01-AB-1234",
                paymentMode = "Online UPI / NEFT"
            )
        }

        val sampleItems = remember {
            listOf(
                GstInvoiceItem(
                    id = "item_1",
                    description = "Enterprise Scanning & CAD Digitization Module",
                    hsnCode = "8471",
                    quantity = 2.0,
                    unit = "PCS",
                    unitPrice = 18000.0,
                    discountPercent = 5.0,
                    gstRatePercent = 18.0,
                    cessPercent = 0.0
                ),
                GstInvoiceItem(
                    id = "item_2",
                    description = "Precision Architectural Calibration Service",
                    hsnCode = "9983",
                    quantity = 1.0,
                    unit = "HRS",
                    unitPrice = 8800.0,
                    discountPercent = 0.0,
                    gstRatePercent = 18.0,
                    cessPercent = 0.0
                )
            )
        }

        Dialog(
            onDismissRequest = { selectedPreviewTemplate = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = GscanBackgroundLight
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = templateToPreview.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Live Layout Preview",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }

                        IconButton(onClick = { selectedPreviewTemplate = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        GstInvoiceTemplatePreview(
                            invoice = sampleInvoice,
                            items = sampleItems,
                            template = templateToPreview
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedPreviewTemplate = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Back")
                        }

                        Button(
                            onClick = {
                                val tmpl = templateToPreview
                                selectedPreviewTemplate = null
                                showTemplateSelectionSheet = false
                                onNavigateToGstInvoiceMaker(tmpl)
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Use This Template", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GstTemplateSelectionModal(
    templates: List<TemplateMeta>,
    onDismiss: () -> Unit,
    onSelectTemplate: (GstInvoiceTemplate) -> Unit,
    onPreviewTemplate: (GstInvoiceTemplate) -> Unit,
    onOpenBlankMaker: () -> Unit
) {
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    val categories = remember {
        listOf("All", "Corporate B2B", "Minimalist", "Engineering & CAD", "Retail POS", "Executive", "Modern Cobalt")
    }

    val filteredTemplates = remember(selectedCategoryFilter) {
        if (selectedCategoryFilter == "All") {
            templates
        } else {
            templates.filter { it.category == selectedCategoryFilter }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Choose GST Invoice Template",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Select from 6 professional GST & E-Way compliant layouts",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategoryFilter == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD97706),
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Color(0xFF334155)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color(0xFFE2E8F0),
                            selectedBorderColor = Color(0xFFD97706)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Template Cards List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredTemplates) { item ->
                    TemplateOptionCard(
                        meta = item,
                        onSelect = { onSelectTemplate(item.template) },
                        onPreview = { onPreviewTemplate(item.template) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick blank invoice footer button
            OutlinedButton(
                onClick = onOpenBlankMaker,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF475569)
                ),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Default Invoice Editor", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun TemplateOptionCard(
    meta: TemplateMeta,
    onSelect: () -> Unit,
    onPreview: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, meta.accentColor.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(meta.accentColor)
                            .border(2.dp, meta.secondaryColor, CircleShape)
                    )

                    Column {
                        Text(
                            text = meta.template.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = meta.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = meta.accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = meta.accentColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "GST READY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = meta.accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = meta.template.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Best for tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Best for: ${meta.bestFor}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Key Feature Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                meta.keyFeatures.forEach { feat ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = meta.accentColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = feat,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF334155),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Preview", fontSize = 12.sp)
                }

                Button(
                    onClick = onSelect,
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = meta.accentColor),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Use Template", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}


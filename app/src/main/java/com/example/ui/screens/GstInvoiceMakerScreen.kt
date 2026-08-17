package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.components.GstInvoiceDataOverlayPreview
import com.example.ui.components.GstInvoiceTemplatePreview
import com.example.ui.theme.*
import com.example.ui.viewmodel.GstInvoiceViewModel
import com.example.utils.GstInvoicePdfEngine
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GstInvoiceMakerScreen(
    viewModel: GstInvoiceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) } // 0: Editor, 1: Templates & Preview, 2: Saved Invoices
    var showProfileDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "GST Invoice Maker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Tax Compliant Billing & Multi-Templates",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF1E293B))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetNewInvoice(); selectedTab = 0 }) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "New Invoice", tint = GscanPrimary)
                    }
                    IconButton(onClick = { showProfileDialog = true }) {
                        Icon(Icons.Default.Business, contentDescription = "Company Profile", tint = Color(0xFF475569))
                    }
                    IconButton(onClick = {
                        viewModel.exportPdf(context) { pdfFile ->
                            GstInvoicePdfEngine.sharePdf(context, pdfFile, "GST Invoice ${uiState.currentInvoice.invoiceNumber}")
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share PDF", tint = Color(0xFF2563EB))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GscanBackgroundLight)
            )
        },
        containerColor = GscanBackgroundLight
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Selector
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = GscanPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Bill", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Templates", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Invoices (${uiState.savedInvoices.size})", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            when (selectedTab) {
                0 -> InvoiceEditorTab(
                    viewModel = viewModel,
                    onOpenAddItem = {
                        editingItemIndex = null
                        showAddItemDialog = true
                    },
                    onEditItem = { idx ->
                        editingItemIndex = idx
                        showAddItemDialog = true
                    },
                    onPreviewClick = { selectedTab = 1 }
                )
                1 -> InvoiceTemplatePreviewTab(
                    viewModel = viewModel,
                    onEditDetails = { selectedTab = 0 }
                )
                2 -> SavedInvoicesListTab(
                    viewModel = viewModel,
                    onSelectInvoice = { invId ->
                        viewModel.loadInvoice(invId)
                        selectedTab = 1
                    }
                )
            }
        }
    }

    if (showAddItemDialog) {
        val initialItem = editingItemIndex?.let { uiState.currentItems.getOrNull(it) } ?: GstInvoiceItem()
        AddItemDialog(
            item = initialItem,
            isEditing = editingItemIndex != null,
            onDismiss = { showAddItemDialog = false },
            onSave = { newItem ->
                if (editingItemIndex != null) {
                    viewModel.updateItem(editingItemIndex!!, newItem)
                } else {
                    viewModel.addItem(newItem)
                }
                showAddItemDialog = false
            }
        )
    }

    if (showProfileDialog) {
        CompanyProfileDialog(
            profile = uiState.businessProfile,
            onDismiss = { showProfileDialog = false },
            onSave = { updated ->
                viewModel.saveBusinessProfile(updated)
                showProfileDialog = false
            }
        )
    }
}

// ---------------- TAB 1: INVOICE EDITOR ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceEditorTab(
    viewModel: GstInvoiceViewModel,
    onOpenAddItem: () -> Unit,
    onEditItem: (Int) -> Unit,
    onPreviewClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val invoice = uiState.currentInvoice
    val items = uiState.currentItems
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val isInterstate = invoice.sellerStateCode.trim() != invoice.buyerStateCode.trim()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Tax Nature Indicator Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isInterstate) Color(0xFFEFF6FF) else Color(0xFFF0FDF4)
                ),
                border = BorderStroke(
                    1.dp,
                    if (isInterstate) Color(0xFF93C5FD) else Color(0xFF86EFAC)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isInterstate) Color(0xFF2563EB) else Color(0xFF16A34A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isInterstate) Icons.Default.SwapHoriz else Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isInterstate) "Interstate Supply (IGST Applicable)" else "Intrastate Supply (CGST + SGST Applicable)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = if (isInterstate) Color(0xFF1E3A8A) else Color(0xFF14532D)
                        )
                        Text(
                            text = if (isInterstate) "Seller (${invoice.sellerStateCode}) & Buyer (${invoice.buyerStateCode}) are in different states. 100% IGST applied."
                            else "Seller & Buyer are in same state (${invoice.sellerStateCode}). Tax split 50% CGST + 50% SGST.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }
        }

        // Invoice Meta Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("INVOICE METADATA", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF64748B), letterSpacing = 1.sp)

                    // Invoice Type Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(GstInvoiceType.values()) { type ->
                            FilterChip(
                                selected = invoice.invoiceType == type.name,
                                onClick = { viewModel.updateInvoiceDetails(invoiceType = type.name) },
                                label = { Text(type.displayName, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = invoice.invoiceNumber,
                            onValueChange = { viewModel.updateInvoiceDetails(invoiceNumber = it) },
                            label = { Text("Invoice No") },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = dateFormat.format(Date(invoice.invoiceDate)),
                            onValueChange = {},
                            label = { Text("Date") },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    val cal = Calendar.getInstance().apply { timeInMillis = invoice.invoiceDate }
                                    DatePickerDialog(context, { _, y, m, d ->
                                        cal.set(y, m, d)
                                        viewModel.updateInvoiceDetails(invoiceDate = cal.timeInMillis)
                                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                                }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = GscanPrimary)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = invoice.eWayBillNo,
                            onValueChange = { viewModel.updateInvoiceDetails(eWayBillNo = it) },
                            label = { Text("E-Way Bill No (Optional)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = invoice.vehicleNo,
                            onValueChange = { viewModel.updateInvoiceDetails(vehicleNo = it) },
                            label = { Text("Vehicle No (Optional)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Buyer Details Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("BUYER / CLIENT DETAILS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF64748B), letterSpacing = 1.sp)
                        Text("GST B2B / B2C", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GscanPrimary)
                    }

                    OutlinedTextField(
                        value = invoice.buyerName,
                        onValueChange = { viewModel.updateInvoiceDetails(buyerName = it) },
                        label = { Text("Buyer Company / Client Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = invoice.buyerGstin,
                            onValueChange = { gstin ->
                                val clean = gstin.uppercase()
                                var stateCode = invoice.buyerStateCode
                                var stateName = invoice.buyerState
                                if (clean.length >= 2) {
                                    val code = clean.substring(0, 2)
                                    val match = INDIAN_GST_STATES.firstOrNull { it.code == code }
                                    if (match != null) {
                                        stateCode = match.code
                                        stateName = match.name
                                    }
                                }
                                viewModel.updateInvoiceDetails(
                                    buyerGstin = clean,
                                    buyerStateCode = stateCode,
                                    buyerState = stateName,
                                    placeOfSupply = "$stateName ($stateCode)"
                                )
                            },
                            label = { Text("Buyer GSTIN") },
                            placeholder = { Text("27AAAAA0000A1Z5") },
                            modifier = Modifier.weight(1.3f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = invoice.buyerStateCode,
                            onValueChange = { code ->
                                val match = INDIAN_GST_STATES.firstOrNull { it.code == code }
                                viewModel.updateInvoiceDetails(
                                    buyerStateCode = code,
                                    buyerState = match?.name ?: invoice.buyerState,
                                    placeOfSupply = "${match?.name ?: invoice.buyerState} ($code)"
                                )
                            },
                            label = { Text("State Code") },
                            modifier = Modifier.weight(0.7f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = invoice.buyerBillingAddress,
                        onValueChange = { viewModel.updateInvoiceDetails(buyerBillingAddress = it) },
                        label = { Text("Billing Address") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = invoice.buyerPhone,
                            onValueChange = { viewModel.updateInvoiceDetails(buyerPhone = it) },
                            label = { Text("Phone") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = invoice.buyerEmail,
                            onValueChange = { viewModel.updateInvoiceDetails(buyerEmail = it) },
                            label = { Text("Email") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Line Items Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ITEMS & SERVICES (${items.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                    Text("HSN/SAC coded tax calculation", fontSize = 10.sp, color = Color(0xFF64748B))
                }
                Button(
                    onClick = onOpenAddItem,
                    colors = ButtonDefaults.buttonColors(containerColor = GscanPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Line Items List
        itemsIndexed(items) { idx, item ->
            val taxRes = item.calculateTaxes(isInterstate)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditItem(idx) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${idx + 1}. ${item.description}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "HSN: ${item.hsnCode} • ${item.quantity} ${item.unit} @ ₹${item.unitPrice}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.removeItem(idx) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "GST ${item.gstRatePercent}% ${if (item.discountPercent > 0) "• ${item.discountPercent}% Disc" else ""}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E40AF),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Taxable: ₹${String.format("%.2f", item.taxableAmount)}", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text("Total: ₹${String.format("%.2f", taxRes.total)}", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        }
                    }
                }
            }
        }

        // Live Total Card
        item {
            var subtotal = 0.0
            var totalCgst = 0.0
            var totalSgst = 0.0
            var totalIgst = 0.0
            var totalCess = 0.0

            for (item in items) {
                val tax = item.calculateTaxes(isInterstate)
                subtotal += item.taxableAmount
                totalCgst += tax.cgst
                totalSgst += tax.sgst
                totalIgst += tax.igst
                totalCess += tax.cess
            }

            val grandTotal = Math.round(subtotal + totalCgst + totalSgst + totalIgst + totalCess).toDouble()

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TAX INVOICE SUMMARY", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    SummaryDarkRow("Taxable Subtotal", "₹ ${String.format("%.2f", subtotal)}")
                    if (!isInterstate) {
                        SummaryDarkRow("CGST (Intrastate)", "₹ ${String.format("%.2f", totalCgst)}")
                        SummaryDarkRow("SGST (Intrastate)", "₹ ${String.format("%.2f", totalSgst)}")
                    } else {
                        SummaryDarkRow("IGST (Interstate)", "₹ ${String.format("%.2f", totalIgst)}")
                    }
                    if (totalCess > 0) SummaryDarkRow("Cess", "₹ ${String.format("%.2f", totalCess)}")

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GRAND TOTAL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("₹ ${String.format("%.2f", grandTotal)}", color = Color(0xFF38BDF8), fontWeight = FontWeight.Black, fontSize = 19.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = GstNumberToWords.convert(grandTotal),
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }

        // Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPreviewClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GscanPrimary)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = GscanPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select Template", color = GscanPrimary, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.saveCurrentInvoice() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GscanPrimary)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Invoice", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------- TAB 2: TEMPLATE SELECTION & LIVE PREVIEW ----------------
@Composable
fun InvoiceTemplatePreviewTab(
    viewModel: GstInvoiceViewModel,
    onEditDetails: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val invoice = uiState.currentInvoice
    val items = uiState.currentItems
    val selectedTemplate = uiState.selectedTemplate

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Template Selector Carousel
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SELECT GST TEMPLATE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF64748B), letterSpacing = 1.sp)
                Text("${GstInvoiceTemplate.values().size} Designs", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GscanPrimary)
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(GstInvoiceTemplate.values()) { template ->
                    val isSelected = selectedTemplate == template
                    Card(
                        modifier = Modifier
                            .width(170.dp)
                            .clickable { viewModel.setTemplate(template) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFEFF6FF) else Color.White
                        ),
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) GscanPrimary else Color(0xFFE2E8F0)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (template) {
                                                GstInvoiceTemplate.CLASSIC_CORPORATE -> Color(0xFF1E3A8A)
                                                GstInvoiceTemplate.MODERN_MINIMAL -> Color(0xFF0F766E)
                                                GstInvoiceTemplate.INDUSTRIAL_ENGINEERING -> Color(0xFF334155)
                                                GstInvoiceTemplate.RETAIL_POS -> Color(0xFF1E293B)
                                                GstInvoiceTemplate.EXECUTIVE_BURGUNDY -> Color(0xFF831843)
                                                GstInvoiceTemplate.VIBRANT_COBALT -> Color(0xFF2563EB)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }

                                if (isSelected) {
                                    Surface(
                                        color = GscanPrimary,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("ACTIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(template.displayName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                            Text(template.description, fontSize = 10.sp, color = Color(0xFF64748B), maxLines = 2, lineHeight = 13.sp)
                        }
                    }
                }
            }
        }

        // Interactive Live Rendered Template View with Dynamic Field Overlays
        GstInvoiceDataOverlayPreview(
            invoice = invoice,
            items = items,
            template = selectedTemplate,
            onTemplateChange = { viewModel.setTemplate(it) },
            onExportPdf = { ctx, callback ->
                viewModel.saveCurrentInvoice {
                    viewModel.exportPdf(ctx, callback)
                }
            },
            onEditFields = onEditDetails
        )
    }
}

// ---------------- TAB 3: SAVED INVOICES ----------------
@Composable
fun SavedInvoicesListTab(
    viewModel: GstInvoiceViewModel,
    onSelectInvoice: (Long) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("ALL") }

    val filtered = remember(uiState.savedInvoices, query, selectedStatus) {
        uiState.savedInvoices.filter {
            (selectedStatus == "ALL" || it.status == selectedStatus) &&
            (query.isBlank() || it.invoiceNumber.contains(query, true) || it.buyerName.contains(query, true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search by invoice # or client name...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Status Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedStatus == "ALL",
                    onClick = { selectedStatus = "ALL" },
                    label = { Text("All (${uiState.savedInvoices.size})") }
                )
            }
            items(GstInvoiceStatus.values()) { status ->
                FilterChip(
                    selected = selectedStatus == status.name,
                    onClick = { selectedStatus = status.name },
                    label = { Text(status.displayName) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No saved invoices found", fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Create a new GST invoice using the 'Edit Bill' tab", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        } else {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(filtered, key = { it.id }) { inv ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectInvoice(inv.id) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("#${inv.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                    Text(inv.buyerName, fontSize = 12.sp, color = Color(0xFF475569), fontWeight = FontWeight.Medium)
                                }

                                Surface(
                                    color = when (inv.status) {
                                        GstInvoiceStatus.PAID.name -> Color(0xFFDCFCE7)
                                        GstInvoiceStatus.OVERDUE.name -> Color(0xFFFEE2E2)
                                        else -> Color(0xFFFEF3C7)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = inv.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (inv.status) {
                                            GstInvoiceStatus.PAID.name -> Color(0xFF16A34A)
                                            GstInvoiceStatus.OVERDUE.name -> Color(0xFFDC2626)
                                            else -> Color(0xFFD97706)
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(dateFormat.format(Date(inv.invoiceDate)), fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("₹ ${String.format("%.2f", inv.grandTotalAmount)}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = GscanPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- ADD / EDIT ITEM DIALOG ----------------
@Composable
fun AddItemDialog(
    item: GstInvoiceItem,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: (GstInvoiceItem) -> Unit
) {
    var description by remember { mutableStateOf(item.description) }
    var hsnCode by remember { mutableStateOf(item.hsnCode) }
    var quantityStr by remember { mutableStateOf(item.quantity.toString()) }
    var unit by remember { mutableStateOf(item.unit) }
    var unitPriceStr by remember { mutableStateOf(if (item.unitPrice > 0) item.unitPrice.toString() else "") }
    var discountStr by remember { mutableStateOf(item.discountPercent.toString()) }
    var selectedGstRate by remember { mutableStateOf(item.gstRatePercent) }
    var cessStr by remember { mutableStateOf(item.cessPercent.toString()) }

    val gstSlabs = listOf(0.0, 5.0, 12.0, 18.0, 28.0)
    val unitOptions = listOf("Pcs", "Box", "Kg", "Mtr", "Nos", "Set", "Job", "Hours", "Sq.Ft", "Litre")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Line Item" else "Add New Item / Service", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Item Name / Description *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hsnCode,
                        onValueChange = { hsnCode = it },
                        label = { Text("HSN / SAC Code") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = unitPriceStr,
                        onValueChange = { unitPriceStr = it },
                        label = { Text("Unit Price (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text("GST Tax Slab Rate", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF64748B))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(gstSlabs) { slab ->
                        FilterChip(
                            selected = selectedGstRate == slab,
                            onClick = { selectedGstRate = slab },
                            label = { Text("${slab.toInt()}%") }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = discountStr,
                        onValueChange = { discountStr = it },
                        label = { Text("Discount %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = cessStr,
                        onValueChange = { cessStr = it },
                        label = { Text("Cess %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (description.isBlank()) return@Button
                    val qty = quantityStr.toDoubleOrNull() ?: 1.0
                    val rate = unitPriceStr.toDoubleOrNull() ?: 0.0
                    val disc = discountStr.toDoubleOrNull() ?: 0.0
                    val cess = cessStr.toDoubleOrNull() ?: 0.0
                    onSave(
                        item.copy(
                            description = description,
                            hsnCode = hsnCode,
                            quantity = qty,
                            unit = unit,
                            unitPrice = rate,
                            discountPercent = disc,
                            gstRatePercent = selectedGstRate,
                            cessPercent = cess
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = GscanPrimary)
            ) {
                Text("Save Item", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ---------------- COMPANY PROFILE DIALOG ----------------
@Composable
fun CompanyProfileDialog(
    profile: GstBusinessProfileEntity,
    onDismiss: () -> Unit,
    onSave: (GstBusinessProfileEntity) -> Unit
) {
    var companyName by remember { mutableStateOf(profile.companyName) }
    var gstin by remember { mutableStateOf(profile.gstin) }
    var pan by remember { mutableStateOf(profile.pan) }
    var address by remember { mutableStateOf(profile.addressLine1) }
    var city by remember { mutableStateOf(profile.city) }
    var state by remember { mutableStateOf(profile.state) }
    var stateCode by remember { mutableStateOf(profile.stateCode) }
    var pinCode by remember { mutableStateOf(profile.pinCode) }
    var phone by remember { mutableStateOf(profile.phone) }
    var email by remember { mutableStateOf(profile.email) }
    var bankName by remember { mutableStateOf(profile.bankName) }
    var accountNo by remember { mutableStateOf(profile.accountNumber) }
    var ifsc by remember { mutableStateOf(profile.ifscCode) }
    var upiId by remember { mutableStateOf(profile.upiId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("My Company Profile & GSTIN", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Company / Business Name *") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = gstin, onValueChange = { gstin = it.uppercase() }, label = { Text("GSTIN *") }, modifier = Modifier.weight(1.2f))
                    OutlinedTextField(value = pan, onValueChange = { pan = it.uppercase() }, label = { Text("PAN") }, modifier = Modifier.weight(0.8f))
                }
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = state, onValueChange = { state = it }, label = { Text("State") }, modifier = Modifier.weight(1.2f))
                    OutlinedTextField(value = stateCode, onValueChange = { stateCode = it }, label = { Text("State Code") }, modifier = Modifier.weight(0.8f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.weight(1f))
                }
                Text("Bank & UPI Details", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF64748B))
                OutlinedTextField(value = bankName, onValueChange = { bankName = it }, label = { Text("Bank Name") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = accountNo, onValueChange = { accountNo = it }, label = { Text("A/C No") }, modifier = Modifier.weight(1.2f))
                    OutlinedTextField(value = ifsc, onValueChange = { ifsc = it.uppercase() }, label = { Text("IFSC") }, modifier = Modifier.weight(0.8f))
                }
                OutlinedTextField(value = upiId, onValueChange = { upiId = it }, label = { Text("UPI ID (for QR payment)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        profile.copy(
                            companyName = companyName,
                            gstin = gstin,
                            pan = pan,
                            addressLine1 = address,
                            city = city,
                            state = state,
                            stateCode = stateCode,
                            pinCode = pinCode,
                            phone = phone,
                            email = email,
                            bankName = bankName,
                            accountNumber = accountNo,
                            ifscCode = ifsc,
                            upiId = upiId
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = GscanPrimary)
            ) {
                Text("Save Profile", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SummaryDarkRow(title: String, amount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 11.sp, color = Color(0xFFCBD5E1))
        Text(amount, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GstInvoiceTemplatePreview(
    invoice: GstInvoiceEntity,
    items: List<GstInvoiceItem>,
    template: GstInvoiceTemplate,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        when (template) {
            GstInvoiceTemplate.CLASSIC_CORPORATE -> ClassicCorporateInvoiceView(invoice, items)
            GstInvoiceTemplate.MODERN_MINIMAL -> ModernMinimalInvoiceView(invoice, items)
            GstInvoiceTemplate.INDUSTRIAL_ENGINEERING -> IndustrialEngineeringInvoiceView(invoice, items)
            GstInvoiceTemplate.RETAIL_POS -> RetailPosInvoiceView(invoice, items)
            GstInvoiceTemplate.EXECUTIVE_BURGUNDY -> ExecutiveBurgundyInvoiceView(invoice, items)
            GstInvoiceTemplate.VIBRANT_COBALT -> VibrantCobaltInvoiceView(invoice, items)
        }
    }
}

// 1. CLASSIC CORPORATE TEMPLATE
@Composable
fun ClassicCorporateInvoiceView(
    invoice: GstInvoiceEntity,
    items: List<GstInvoiceItem>
) {
    val primaryColor = Color(0xFF1E3A8A) // Navy Blue
    val secondaryColor = Color(0xFFEFF6FF)
    val accentGold = Color(0xFFD97706)
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val isInterstate = invoice.sellerStateCode.trim() != invoice.buyerStateCode.trim()

    Column(modifier = Modifier.fillMaxWidth()) {
        // Corporate Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(primaryColor)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invoice.sellerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "GSTIN: ${invoice.sellerGstin} • PAN: ${invoice.sellerPan}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${invoice.sellerPhone} • ${invoice.sellerEmail}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = accentGold,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = invoice.invoiceType.replace("_", " "),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Inv: ${invoice.invoiceNumber}",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Date: ${dateFormat.format(Date(invoice.invoiceDate))}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Gold Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(accentGold)
        )

        // Billed by & Billed to
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Seller Info
            Surface(
                modifier = Modifier.weight(1f),
                color = secondaryColor,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.8.dp, primaryColor.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("SUPPLIED BY", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = primaryColor)
                    Text(invoice.sellerName, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color(0xFF0F172A))
                    Text(invoice.sellerAddress, fontSize = 10.sp, color = Color(0xFF475569), lineHeight = 13.sp)
                    Text("State: ${invoice.sellerState} (${invoice.sellerStateCode})", fontSize = 10.sp, color = Color(0xFF334155))
                }
            }

            // Buyer Info
            Surface(
                modifier = Modifier.weight(1f),
                color = secondaryColor,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.8.dp, primaryColor.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("BILLED TO", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = primaryColor)
                    Text(invoice.buyerName, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color(0xFF0F172A))
                    Text(invoice.buyerBillingAddress, fontSize = 10.sp, color = Color(0xFF475569), lineHeight = 13.sp)
                    Text("GSTIN: ${if (invoice.buyerGstin.isNotBlank()) invoice.buyerGstin else "URP"}", fontSize = 10.sp, color = Color(0xFF334155))
                    Text("Place of Supply: ${invoice.placeOfSupply}", fontSize = 10.sp, color = Color(0xFF334155), fontWeight = FontWeight.Medium)
                }
            }
        }

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .background(primaryColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.width(18.dp))
            Text("Item / Description", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.8f))
            Text("HSN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.9f))
            Text("Qty", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.8f))
            Text("Rate (₹)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
            Text("Taxable", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
            Text("GST%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
            Text("Total (₹)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
        }

        // Table Rows
        var subtotal = 0.0
        var totalCgst = 0.0
        var totalSgst = 0.0
        var totalIgst = 0.0
        var totalCess = 0.0

        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            items.forEachIndexed { idx, item ->
                val taxRes = item.calculateTaxes(isInterstate)
                subtotal += item.taxableAmount
                totalCgst += taxRes.cgst
                totalSgst += taxRes.sgst
                totalIgst += taxRes.igst
                totalCess += taxRes.cess

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (idx % 2 == 1) Color(0xFFF8FAFC) else Color.White)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${idx + 1}", fontSize = 10.sp, color = Color(0xFF475569), modifier = Modifier.width(18.dp))
                    Text(item.description, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A), modifier = Modifier.weight(1.8f))
                    Text(item.hsnCode, fontSize = 10.sp, color = Color(0xFF64748B), modifier = Modifier.weight(0.9f))
                    Text("${item.quantity} ${item.unit}", fontSize = 10.sp, color = Color(0xFF334155), modifier = Modifier.weight(0.8f))
                    Text(String.format("%.2f", item.unitPrice), fontSize = 10.sp, color = Color(0xFF334155), modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                    Text(String.format("%.2f", item.taxableAmount), fontSize = 10.sp, color = Color(0xFF334155), modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                    Text("${item.gstRatePercent}%", fontSize = 10.sp, color = Color(0xFF334155), modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                    Text(String.format("%.2f", taxRes.total), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A), modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
                }
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.6.dp)
            }
        }

        val rawGrandTotal = subtotal + totalCgst + totalSgst + totalIgst + totalCess
        val finalGrandTotal = Math.round(rawGrandTotal * 100.0) / 100.0
        val roundOff = Math.round((Math.round(finalGrandTotal) - finalGrandTotal) * 100.0) / 100.0
        val payableTotal = Math.round(finalGrandTotal).toDouble()

        // Summary Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bank Details
            if (invoice.showBankDetails) {
                Surface(
                    modifier = Modifier.weight(1.1f),
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.8.dp, Color(0xFFCBD5E1))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("BANK DETAILS", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = primaryColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Bank: ${invoice.sellerBankName}", fontSize = 10.sp, color = Color(0xFF334155))
                        Text("A/C: ${invoice.sellerAccountNo}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        Text("IFSC: ${invoice.sellerIfsc}", fontSize = 10.sp, color = Color(0xFF334155))
                        Text("UPI ID: ${invoice.sellerUpi}", fontSize = 10.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Totals Summary Box
            Surface(
                modifier = Modifier.weight(1f),
                color = secondaryColor,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.8.dp, primaryColor.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    SummaryRow("Taxable Amount", "₹ ${String.format("%.2f", subtotal)}")
                    if (!isInterstate) {
                        SummaryRow("CGST", "₹ ${String.format("%.2f", totalCgst)}")
                        SummaryRow("SGST", "₹ ${String.format("%.2f", totalSgst)}")
                    } else {
                        SummaryRow("IGST", "₹ ${String.format("%.2f", totalIgst)}")
                    }
                    if (totalCess > 0) SummaryRow("Cess", "₹ ${String.format("%.2f", totalCess)}")
                    SummaryRow("Round Off", "₹ ${String.format("%.2f", roundOff)}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(primaryColor, RoundedCornerShape(4.dp))
                            .padding(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("TOTAL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("₹ ${String.format("%.2f", payableTotal)}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Amount in Words
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            color = Color(0xFFF1F5F9),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = "Amount in Words: ${GstNumberToWords.convert(payableTotal)}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }

        // Terms & Signatory
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("TERMS & CONDITIONS:", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color(0xFF64748B))
                Text(invoice.terms, fontSize = 8.5.sp, color = Color(0xFF475569), lineHeight = 11.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("For ${invoice.sellerName}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Spacer(modifier = Modifier.height(28.dp))
                HorizontalDivider(modifier = Modifier.width(120.dp), color = Color.Gray, thickness = 1.dp)
                Text("Authorized Signatory", fontSize = 9.sp, color = Color(0xFF475569))
            }
        }
    }
}

// 2. MODERN MINIMAL TEMPLATE
@Composable
fun ModernMinimalInvoiceView(
    invoice: GstInvoiceEntity,
    items: List<GstInvoiceItem>
) {
    val primaryTeal = Color(0xFF0F766E)
    val lightTeal = Color(0xFFF0FDFA)
    val isInterstate = invoice.sellerStateCode.trim() != invoice.buyerStateCode.trim()
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
        // Clean Minimal Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(invoice.sellerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = primaryTeal)
                Text("GSTIN: ${invoice.sellerGstin}", fontSize = 11.sp, color = Color(0xFF475569))
                Text("${invoice.sellerPhone} | ${invoice.sellerEmail}", fontSize = 10.sp, color = Color(0xFF64748B))
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = primaryTeal,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "INVOICE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("#${invoice.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                Text(dateFormat.format(Date(invoice.invoiceDate)), fontSize = 11.sp, color = Color(0xFF64748B))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Buyer Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = lightTeal,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, primaryTeal.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("CLIENT DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryTeal)
                    Text(invoice.buyerName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                    Text(invoice.buyerBillingAddress, fontSize = 10.sp, color = Color(0xFF475569))
                    Text("GSTIN: ${if (invoice.buyerGstin.isNotBlank()) invoice.buyerGstin else "URP"}", fontSize = 10.sp, color = Color(0xFF0F172A))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("PLACE OF SUPPLY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryTeal)
                    Text(invoice.placeOfSupply, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color(0xFF0F172A))
                    Text("Due Date: ${dateFormat.format(Date(invoice.dueDate))}", fontSize = 10.sp, color = Color(0xFF64748B))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Line Items
        var subtotal = 0.0
        var totalTax = 0.0

        items.forEachIndexed { index, item ->
            val taxRes = item.calculateTaxes(isInterstate)
            subtotal += item.taxableAmount
            totalTax += (taxRes.cgst + taxRes.sgst + taxRes.igst + taxRes.cess)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.description, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
                        Text("HSN: ${item.hsnCode} • ${item.quantity} ${item.unit} @ ₹${item.unitPrice} (GST ${item.gstRatePercent}%)", fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                    Text("₹ ${String.format("%.2f", taxRes.total)}", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = primaryTeal)
                }
            }
        }

        val grandTotal = subtotal + totalTax
        val payableTotal = Math.round(grandTotal).toDouble()

        Spacer(modifier = Modifier.height(10.dp))

        // Total & Payment Badge
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = primaryTeal,
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("TOTAL AMOUNT DUE", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("₹ ${String.format("%.2f", payableTotal)}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isInterstate) "Interstate IGST Applied" else "Intrastate GST (CGST+SGST)",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// 3. INDUSTRIAL & CAD ENGINEERING TEMPLATE
@Composable
fun IndustrialEngineeringInvoiceView(
    invoice: GstInvoiceEntity,
    items: List<GstInvoiceItem>
) {
    val darkSlate = Color(0xFF1E293B)
    val blueprintBlue = Color(0xFF0369A1)
    val isInterstate = invoice.sellerStateCode.trim() != invoice.buyerStateCode.trim()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, darkSlate)
    ) {
        // Blueprint Technical Title Block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(darkSlate)
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(invoice.sellerName.uppercase(), fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                Text("CAD & TECHNICAL BILLING SPECIFICATION", fontSize = 9.sp, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
            }
            Surface(
                color = blueprintBlue,
                shape = RoundedCornerShape(2.dp)
            ) {
                Text(
                    text = "SPEC NO: ${invoice.invoiceNumber}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Meta Specs (E-Way Bill, Vehicle, PoS)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("GSTIN: ${invoice.sellerGstin}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text("E-WAY: ${if (invoice.eWayBillNo.isNotBlank()) invoice.eWayBillNo else "N/A"}", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text("VEHICLE: ${if (invoice.vehicleNo.isNotBlank()) invoice.vehicleNo else "N/A"}", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text("DATE: ${dateFormat.format(Date(invoice.invoiceDate))}", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }

        HorizontalDivider(color = darkSlate, thickness = 1.dp)

        // Item List Monospace
        var subtotal = 0.0
        var totalTax = 0.0

        items.forEachIndexed { i, item ->
            val tax = item.calculateTaxes(isInterstate)
            subtotal += item.taxableAmount
            totalTax += (tax.cgst + tax.sgst + tax.igst + tax.cess)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("[0${i+1}] ${item.description.uppercase()}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("HSN:${item.hsnCode} | QTY:${item.quantity} ${item.unit} | RATE:₹${item.unitPrice} | GST:${item.gstRatePercent}%", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.DarkGray)
                }
                Text("₹${String.format("%.2f", tax.total)}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
            }
            HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 0.5.dp)
        }

        val totalPayable = Math.round(subtotal + totalTax).toDouble()

        // Engineering Totals
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(darkSlate)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("NET AUDITED AMOUNT (INR):", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("₹ ${String.format("%.2f", totalPayable)}", color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

// 4. RETAIL POS THERMAL BILL
@Composable
fun RetailPosInvoiceView(
    invoice: GstInvoiceEntity,
    items: List<GstInvoiceItem>
) {
    val isInterstate = invoice.sellerStateCode.trim() != invoice.buyerStateCode.trim()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(invoice.sellerName.uppercase(), fontWeight = FontWeight.Black, fontSize = 13.sp, textAlign = TextAlign.Center)
        Text(invoice.sellerAddress, fontSize = 9.sp, color = Color.DarkGray, textAlign = TextAlign.Center)
        Text("GSTIN: ${invoice.sellerGstin} • Ph: ${invoice.sellerPhone}", fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        
        Spacer(modifier = Modifier.height(6.dp))
        DashedDivider()
        Spacer(modifier = Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("BILL NO: ${invoice.invoiceNumber}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(dateFormat.format(Date(invoice.invoiceDate)), fontSize = 9.sp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("CUSTOMER: ${invoice.buyerName}", fontSize = 9.sp)
            Text("PoS: ${invoice.buyerStateCode}", fontSize = 9.sp)
        }

        Spacer(modifier = Modifier.height(4.dp))
        DashedDivider()
        Spacer(modifier = Modifier.height(4.dp))

        // POS Items
        var subtotal = 0.0
        var totalTax = 0.0

        items.forEach { item ->
            val tax = item.calculateTaxes(isInterstate)
            subtotal += item.taxableAmount
            totalTax += (tax.cgst + tax.sgst + tax.igst + tax.cess)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${item.description} (x${item.quantity})", fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text("₹${String.format("%.2f", tax.total)}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        DashedDivider()
        Spacer(modifier = Modifier.height(6.dp))

        val grandTotal = Math.round(subtotal + totalTax).toDouble()

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SUBTOTAL:", fontSize = 10.sp)
            Text("₹ ${String.format("%.2f", subtotal)}", fontSize = 10.sp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("GST TAX BREAKUP:", fontSize = 10.sp)
            Text("₹ ${String.format("%.2f", totalTax)}", fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("NET PAYABLE:", fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text("₹ ${String.format("%.2f", grandTotal)}", fontSize = 14.sp, fontWeight = FontWeight.Black)
        }

        Spacer(modifier = Modifier.height(8.dp))
        DashedDivider()
        Spacer(modifier = Modifier.height(6.dp))
        Text("Scan & Pay UPI: ${invoice.sellerUpi}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
        Text("Thank You! Please Visit Again.", fontSize = 9.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
    }
}

// 5. EXECUTIVE BURGUNDY TEMPLATE
@Composable
fun ExecutiveBurgundyInvoiceView(
    invoice: GstInvoiceEntity,
    items: List<GstInvoiceItem>
) {
    val burgundy = Color(0xFF831843)
    val gold = Color(0xFFD97706)
    val isInterstate = invoice.sellerStateCode.trim() != invoice.buyerStateCode.trim()
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(burgundy)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(invoice.sellerName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Text("GSTIN: ${invoice.sellerGstin}", fontSize = 10.sp, color = Color(0xFFFCE7F3))
                    Text(invoice.sellerEmail, fontSize = 10.sp, color = Color(0xFFFCE7F3))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TAX INVOICE", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = gold)
                    Text("#${invoice.invoiceNumber}", fontSize = 11.sp, color = Color.White)
                    Text(dateFormat.format(Date(invoice.invoiceDate)), fontSize = 10.sp, color = Color(0xFFFCE7F3))
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(gold))

        // Content
        Column(modifier = Modifier.padding(14.dp)) {
            Text("INVOICE TO:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = burgundy)
            Text(invoice.buyerName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
            Text(invoice.buyerBillingAddress, fontSize = 10.sp, color = Color(0xFF475569))
            Text("GSTIN: ${if (invoice.buyerGstin.isNotBlank()) invoice.buyerGstin else "Unregistered"}", fontSize = 10.sp)

            Spacer(modifier = Modifier.height(10.dp))

            var subtotal = 0.0
            var totalTax = 0.0

            items.forEachIndexed { idx, item ->
                val tax = item.calculateTaxes(isInterstate)
                subtotal += item.taxableAmount
                totalTax += (tax.cgst + tax.sgst + tax.igst + tax.cess)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${idx+1}. ${item.description}", fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text("₹ ${String.format("%.2f", tax.total)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = burgundy)
                }
                HorizontalDivider(color = Color(0xFFF1F5F9))
            }

            val grandTotal = Math.round(subtotal + totalTax).toDouble()

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFDF2F8),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, burgundy.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("GRAND TOTAL", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = burgundy)
                    Text("₹ ${String.format("%.2f", grandTotal)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = burgundy)
                }
            }
        }
    }
}

// 6. VIBRANT COBALT TEMPLATE
@Composable
fun VibrantCobaltInvoiceView(
    invoice: GstInvoiceEntity,
    items: List<GstInvoiceItem>
) {
    val cobalt = Color(0xFF2563EB)
    val lightCobalt = Color(0xFFEFF6FF)
    val isInterstate = invoice.sellerStateCode.trim() != invoice.buyerStateCode.trim()
    val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cobalt)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(invoice.sellerName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Text("GSTIN: ${invoice.sellerGstin}", fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f))
                }
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "INV #${invoice.invoiceNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = cobalt,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(14.dp)) {
            // Client card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = lightCobalt,
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("BILL TO", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = cobalt)
                    Text(invoice.buyerName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                    Text("GSTIN: ${invoice.buyerGstin} • State: ${invoice.buyerState}", fontSize = 10.sp, color = Color(0xFF475569))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            var subtotal = 0.0
            var totalTax = 0.0

            items.forEach { item ->
                val tax = item.calculateTaxes(isInterstate)
                subtotal += item.taxableAmount
                totalTax += (tax.cgst + tax.sgst + tax.igst + tax.cess)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.description, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("${item.quantity} ${item.unit} x ₹${item.unitPrice}", fontSize = 9.5.sp, color = Color.Gray)
                    }
                    Text("₹ ${String.format("%.2f", tax.total)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = cobalt)
                }
            }

            val grandTotal = Math.round(subtotal + totalTax).toDouble()

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = cobalt,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TOTAL AMOUNT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("₹ ${String.format("%.2f", grandTotal)}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun SummaryRow(title: String, amount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 9.sp, color = Color(0xFF475569))
        Text(amount, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
    }
}

@Composable
fun DashedDivider(modifier: Modifier = Modifier, color: Color = Color.Gray) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color.copy(alpha = 0.4f))
    )
}

package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.*
import com.example.data.repository.GstInvoiceRepository
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object GstInvoicePdfEngine {

    fun generateInvoicePdf(
        context: Context,
        invoice: GstInvoiceEntity,
        items: List<GstInvoiceItem>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (595 x 842 pt)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val template = try {
            GstInvoiceTemplate.values().firstOrNull { it.id == invoice.templateId } ?: GstInvoiceTemplate.CLASSIC_CORPORATE
        } catch (e: Exception) {
            GstInvoiceTemplate.CLASSIC_CORPORATE
        }

        renderInvoiceToCanvas(canvas, invoice, items, template, 595f, 842f)
        pdfDocument.finishPage(page)

        val outputDir = File(context.filesDir, "gst_invoices").apply { mkdirs() }
        val sanitizedNumber = invoice.invoiceNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val pdfFile = File(outputDir, "Invoice_${sanitizedNumber}_${System.currentTimeMillis()}.pdf")

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    private fun renderInvoiceToCanvas(
        canvas: Canvas,
        invoice: GstInvoiceEntity,
        items: List<GstInvoiceItem>,
        template: GstInvoiceTemplate,
        width: Float,
        height: Float
    ) {
        // Base Canvas setup
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY }
        
        // Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, width, height, paint)

        val isInterstate = invoice.sellerStateCode.trim() != invoice.buyerStateCode.trim()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val primaryColor = when (template) {
            GstInvoiceTemplate.CLASSIC_CORPORATE -> Color.parseColor("#1E3A8A") // Navy Blue
            GstInvoiceTemplate.MODERN_MINIMAL -> Color.parseColor("#0F766E") // Teal / Emerald
            GstInvoiceTemplate.INDUSTRIAL_ENGINEERING -> Color.parseColor("#334155") // Slate / Charcoal
            GstInvoiceTemplate.RETAIL_POS -> Color.parseColor("#1E293B") // Dark Slate
            GstInvoiceTemplate.EXECUTIVE_BURGUNDY -> Color.parseColor("#831843") // Deep Burgundy / Wine
            GstInvoiceTemplate.VIBRANT_COBALT -> Color.parseColor("#2563EB") // Electric Cobalt
        }

        val secondaryColor = when (template) {
            GstInvoiceTemplate.CLASSIC_CORPORATE -> Color.parseColor("#DBEAFE")
            GstInvoiceTemplate.MODERN_MINIMAL -> Color.parseColor("#CCFBF1")
            GstInvoiceTemplate.INDUSTRIAL_ENGINEERING -> Color.parseColor("#E2E8F0")
            GstInvoiceTemplate.RETAIL_POS -> Color.parseColor("#F1F5F9")
            GstInvoiceTemplate.EXECUTIVE_BURGUNDY -> Color.parseColor("#FCE7F3")
            GstInvoiceTemplate.VIBRANT_COBALT -> Color.parseColor("#EFF6FF")
        }

        val accentGold = Color.parseColor("#D97706")

        // 1. Header Banner
        paint.color = primaryColor
        when (template) {
            GstInvoiceTemplate.CLASSIC_CORPORATE, GstInvoiceTemplate.VIBRANT_COBALT -> {
                canvas.drawRect(0f, 0f, width, 85f, paint)
                // Gold accent bar
                paint.color = accentGold
                canvas.drawRect(0f, 85f, width, 88f, paint)
            }
            GstInvoiceTemplate.MODERN_MINIMAL -> {
                canvas.drawRect(0f, 0f, width, 75f, paint)
            }
            GstInvoiceTemplate.INDUSTRIAL_ENGINEERING -> {
                canvas.drawRect(0f, 0f, width, 80f, paint)
                paint.color = Color.DKGRAY
                paint.strokeWidth = 1.5f
                canvas.drawLine(0f, 80f, width, 80f, paint)
            }
            GstInvoiceTemplate.EXECUTIVE_BURGUNDY -> {
                canvas.drawRect(0f, 0f, width, 90f, paint)
                paint.color = Color.parseColor("#F59E0B")
                canvas.drawRect(20f, 88f, width - 20f, 90f, paint)
            }
            GstInvoiceTemplate.RETAIL_POS -> {
                paint.color = Color.parseColor("#1E293B")
                canvas.drawRect(0f, 0f, width, 65f, paint)
            }
        }

        // Header Text: Company Name & Document Title
        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(invoice.sellerName, 24f, 32f, paint)

        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("${invoice.sellerAddress} | GSTIN: ${invoice.sellerGstin}", 24f, 48f, paint)
        canvas.drawText("Phone: ${invoice.sellerPhone} | Email: ${invoice.sellerEmail}", 24f, 62f, paint)

        // Title Tag on right
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val invoiceTypeTitle = when (invoice.invoiceType) {
            GstInvoiceType.BILL_OF_SUPPLY.name -> "BILL OF SUPPLY"
            GstInvoiceType.PROFORMA_INVOICE.name -> "PROFORMA INVOICE"
            GstInvoiceType.EXPORT_INVOICE.name -> "EXPORT INVOICE"
            GstInvoiceType.QUOTATION.name -> "QUOTATION"
            else -> "TAX INVOICE"
        }
        val titleWidth = paint.measureText(invoiceTypeTitle)
        canvas.drawText(invoiceTypeTitle, width - titleWidth - 24f, 34f, paint)

        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        val invNoText = "Invoice No: ${invoice.invoiceNumber}"
        val invNoWidth = paint.measureText(invNoText)
        canvas.drawText(invNoText, width - invNoWidth - 24f, 50f, paint)

        val dateText = "Date: ${dateFormat.format(Date(invoice.invoiceDate))}"
        val dateWidth = paint.measureText(dateText)
        canvas.drawText(dateText, width - dateWidth - 24f, 64f, paint)

        var currentY = 104f

        // 2. Info Cards (Billed By & Billed To)
        val cardWidth = (width - 48f - 12f) / 2f
        val cardHeight = 82f

        // Billed By Box
        paint.color = secondaryColor
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(24f, currentY, 24f + cardWidth, currentY + cardHeight), 6f, 6f, paint)
        paint.color = primaryColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(RectF(24f, currentY, 24f + cardWidth, currentY + cardHeight), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = primaryColor
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BILLED BY (SUPPLIER)", 32f, currentY + 16f, paint)

        paint.color = Color.BLACK
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(invoice.sellerName, 32f, currentY + 30f, paint)

        paint.color = Color.DKGRAY
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        val sellerAddrCut = if (invoice.sellerAddress.length > 40) invoice.sellerAddress.take(40) + "..." else invoice.sellerAddress
        canvas.drawText(sellerAddrCut, 32f, currentY + 44f, paint)
        canvas.drawText("GSTIN: ${invoice.sellerGstin}  |  PAN: ${invoice.sellerPan}", 32f, currentY + 58f, paint)
        canvas.drawText("State: ${invoice.sellerState} (Code: ${invoice.sellerStateCode})", 32f, currentY + 72f, paint)

        // Billed To Box
        val buyerX = 24f + cardWidth + 12f
        paint.color = secondaryColor
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(buyerX, currentY, buyerX + cardWidth, currentY + cardHeight), 6f, 6f, paint)
        paint.color = primaryColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(RectF(buyerX, currentY, buyerX + cardWidth, currentY + cardHeight), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = primaryColor
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BILLED TO (BUYER)", buyerX + 8f, currentY + 16f, paint)

        paint.color = Color.BLACK
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(invoice.buyerName, buyerX + 8f, currentY + 30f, paint)

        paint.color = Color.DKGRAY
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        val buyerAddrCut = if (invoice.buyerBillingAddress.length > 40) invoice.buyerBillingAddress.take(40) + "..." else invoice.buyerBillingAddress
        canvas.drawText(buyerAddrCut, buyerX + 8f, currentY + 44f, paint)
        canvas.drawText("GSTIN: ${if (invoice.buyerGstin.isNotBlank()) invoice.buyerGstin else "URP (Unregistered)"}  |  PAN: ${invoice.buyerPan}", buyerX + 8f, currentY + 58f, paint)
        canvas.drawText("State: ${invoice.buyerState} (Code: ${invoice.buyerStateCode}) | PoS: ${invoice.placeOfSupply}", buyerX + 8f, currentY + 72f, paint)

        currentY += cardHeight + 14f

        // 3. Line Items Table Header
        paint.color = primaryColor
        canvas.drawRoundRect(RectF(24f, currentY, width - 24f, currentY + 22f), 4f, 4f, paint)

        paint.color = Color.WHITE
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("#", 28f, currentY + 14f, paint)
        canvas.drawText("Item / Description", 44f, currentY + 14f, paint)
        canvas.drawText("HSN/SAC", 210f, currentY + 14f, paint)
        canvas.drawText("Qty", 260f, currentY + 14f, paint)
        canvas.drawText("Rate (₹)", 300f, currentY + 14f, paint)
        canvas.drawText("Disc%", 350f, currentY + 14f, paint)
        canvas.drawText("Taxable", 390f, currentY + 14f, paint)
        canvas.drawText("GST%", 445f, currentY + 14f, paint)
        canvas.drawText("Total (₹)", 500f, currentY + 14f, paint)

        currentY += 24f

        // 4. Line Items Rows
        var subtotal = 0.0
        var totalCgst = 0.0
        var totalSgst = 0.0
        var totalIgst = 0.0
        var totalCess = 0.0

        for (idx in items.indices) {
            val item = items[idx]
            val taxRes = item.calculateTaxes(isInterstate)
            subtotal += item.taxableAmount
            totalCgst += taxRes.cgst
            totalSgst += taxRes.sgst
            totalIgst += taxRes.igst
            totalCess += taxRes.cess

            // Row background alternate
            if (idx % 2 == 1) {
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(24f, currentY, width - 24f, currentY + 18f, paint)
            }

            paint.color = Color.parseColor("#334155")
            paint.textSize = 8f
            paint.typeface = Typeface.DEFAULT

            canvas.drawText("${idx + 1}", 28f, currentY + 12f, paint)
            val descCut = if (item.description.length > 32) item.description.take(30) + ".." else item.description
            canvas.drawText(descCut, 44f, currentY + 12f, paint)
            canvas.drawText(item.hsnCode, 210f, currentY + 12f, paint)
            canvas.drawText("${item.quantity} ${item.unit}", 260f, currentY + 12f, paint)
            canvas.drawText(String.format("%.2f", item.unitPrice), 300f, currentY + 12f, paint)
            canvas.drawText("${item.discountPercent}%", 350f, currentY + 12f, paint)
            canvas.drawText(String.format("%.2f", item.taxableAmount), 390f, currentY + 12f, paint)
            canvas.drawText("${item.gstRatePercent}%", 445f, currentY + 12f, paint)
            canvas.drawText(String.format("%.2f", taxRes.total), 500f, currentY + 12f, paint)

            paint.color = Color.parseColor("#E2E8F0")
            paint.strokeWidth = 0.5f
            canvas.drawLine(24f, currentY + 18f, width - 24f, currentY + 18f, paint)

            currentY += 19f
        }

        val rawGrandTotal = subtotal + totalCgst + totalSgst + totalIgst + totalCess
        val grandTotalRounded = Math.round(rawGrandTotal * 100.0) / 100.0
        val roundOff = Math.round((Math.round(grandTotalRounded) - grandTotalRounded) * 100.0) / 100.0
        val finalPayable = Math.round(grandTotalRounded).toDouble()

        currentY += 6f

        // 5. Bottom Section: Bank Info on Left, Totals on Right
        val totalsBoxWidth = 200f
        val totalsX = width - 24f - totalsBoxWidth

        // Bank Details & Payment Info
        if (invoice.showBankDetails) {
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRoundRect(RectF(24f, currentY, totalsX - 12f, currentY + 95f), 6f, 6f, paint)
            paint.color = Color.parseColor("#CBD5E1")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.7f
            canvas.drawRoundRect(RectF(24f, currentY, totalsX - 12f, currentY + 95f), 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            paint.color = primaryColor
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("BANK DETAILS & PAYMENT", 32f, currentY + 16f, paint)

            paint.color = Color.DKGRAY
            paint.textSize = 8f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Bank: ${invoice.sellerBankName}", 32f, currentY + 30f, paint)
            canvas.drawText("A/C No: ${invoice.sellerAccountNo}", 32f, currentY + 44f, paint)
            canvas.drawText("IFSC: ${invoice.sellerIfsc}", 32f, currentY + 58f, paint)
            canvas.drawText("UPI ID: ${invoice.sellerUpi}", 32f, currentY + 72f, paint)
            canvas.drawText("Payment Terms: Due by ${dateFormat.format(Date(invoice.dueDate))}", 32f, currentY + 86f, paint)
        }

        // Totals Card on Right
        paint.color = secondaryColor
        canvas.drawRoundRect(RectF(totalsX, currentY, width - 24f, currentY + 115f), 6f, 6f, paint)
        paint.color = primaryColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(RectF(totalsX, currentY, width - 24f, currentY + 115f), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        var ty = currentY + 16f
        paint.color = Color.BLACK
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT

        canvas.drawText("Taxable Subtotal:", totalsX + 8f, ty, paint)
        canvas.drawText("₹ ${String.format("%.2f", subtotal)}", width - 32f - paint.measureText("₹ ${String.format("%.2f", subtotal)}"), ty, paint)

        if (!isInterstate) {
            ty += 14f
            canvas.drawText("CGST (Intrastate):", totalsX + 8f, ty, paint)
            canvas.drawText("₹ ${String.format("%.2f", totalCgst)}", width - 32f - paint.measureText("₹ ${String.format("%.2f", totalCgst)}"), ty, paint)

            ty += 14f
            canvas.drawText("SGST (Intrastate):", totalsX + 8f, ty, paint)
            canvas.drawText("₹ ${String.format("%.2f", totalSgst)}", width - 32f - paint.measureText("₹ ${String.format("%.2f", totalSgst)}"), ty, paint)
        } else {
            ty += 14f
            canvas.drawText("IGST (Interstate):", totalsX + 8f, ty, paint)
            canvas.drawText("₹ ${String.format("%.2f", totalIgst)}", width - 32f - paint.measureText("₹ ${String.format("%.2f", totalIgst)}"), ty, paint)
        }

        if (totalCess > 0) {
            ty += 14f
            canvas.drawText("Cess:", totalsX + 8f, ty, paint)
            canvas.drawText("₹ ${String.format("%.2f", totalCess)}", width - 32f - paint.measureText("₹ ${String.format("%.2f", totalCess)}"), ty, paint)
        }

        ty += 14f
        canvas.drawText("Round Off:", totalsX + 8f, ty, paint)
        canvas.drawText("₹ ${String.format("%.2f", roundOff)}", width - 32f - paint.measureText("₹ ${String.format("%.2f", roundOff)}"), ty, paint)

        ty += 6f
        paint.color = primaryColor
        canvas.drawRect(totalsX + 4f, ty, width - 28f, ty + 24f, paint)

        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("GRAND TOTAL:", totalsX + 8f, ty + 16f, paint)
        val grandTotalText = "₹ ${String.format("%.2f", finalPayable)}"
        canvas.drawText(grandTotalText, width - 32f - paint.measureText(grandTotalText), ty + 16f, paint)

        currentY += 124f

        // 6. Amount in Words
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(RectF(24f, currentY, width - 24f, currentY + 22f), 4f, 4f, paint)
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val amountWords = GstNumberToWords.convert(finalPayable)
        canvas.drawText("Amount in Words: $amountWords", 30f, currentY + 14f, paint)

        currentY += 30f

        // 7. Terms & Conditions and Signature Area
        val termsWidth = (width - 48f) * 0.6f
        paint.color = Color.DKGRAY
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TERMS & CONDITIONS:", 24f, currentY, paint)

        paint.textSize = 7.5f
        paint.typeface = Typeface.DEFAULT
        val termsLines = invoice.terms.lines()
        var termY = currentY + 12f
        for (line in termsLines.take(3)) {
            canvas.drawText(line, 24f, termY, paint)
            termY += 10f
        }

        // Authorized Signatory on Bottom Right
        val sigX = width - 170f
        paint.color = Color.DKGRAY
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("For ${invoice.sellerName}", sigX, currentY + 10f, paint)

        paint.strokeWidth = 1f
        paint.color = Color.LTGRAY
        canvas.drawLine(sigX, currentY + 45f, width - 24f, currentY + 45f, paint)

        paint.color = Color.DKGRAY
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Authorized Signatory", sigX + 15f, currentY + 56f, paint)

        // Footer note
        paint.color = Color.GRAY
        paint.textSize = 7f
        val footerText = "Generated via Scanvoro GST Invoice Maker • Digitally compliant with GST rules"
        val footerWidth = paint.measureText(footerText)
        canvas.drawText(footerText, (width - footerWidth) / 2f, height - 14f, paint)
    }

    fun sharePdf(context: Context, pdfFile: File, title: String = "GST Tax Invoice") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Here is your GST Tax Invoice from Scanvoro.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share GST Invoice PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}

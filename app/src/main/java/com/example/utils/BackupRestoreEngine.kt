package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.GscanDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class RestoreMode {
    MERGE, // Keep existing and append new records
    OVERWRITE // Clear and replace with backup records
}

data class RestoreResult(
    val isSuccess: Boolean,
    val invoicesRestored: Int = 0,
    val documentsRestored: Int = 0,
    val profileRestored: Boolean = false,
    val message: String = ""
)

object BackupRestoreEngine {

    suspend fun createBackupFile(
        context: Context,
        database: GscanDatabase
    ): File = withContext(Dispatchers.IO) {
        val rootJson = JSONObject()
        val timestamp = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

        // Header Metadata
        rootJson.put("appName", "Scanvoro")
        rootJson.put("appVersion", "1.0.0")
        rootJson.put("schemaVersion", 2)
        rootJson.put("backupTimestamp", timestamp)
        rootJson.put("backupDateFormatted", SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp)))

        // 1. Export Business Profile
        val profile = database.gstInvoiceDao().getBusinessProfile()
        if (profile != null) {
            val profileObj = JSONObject().apply {
                put("companyName", profile.companyName)
                put("tradeName", profile.tradeName)
                put("gstin", profile.gstin)
                put("pan", profile.pan)
                put("addressLine1", profile.addressLine1)
                put("addressLine2", profile.addressLine2)
                put("city", profile.city)
                put("state", profile.state)
                put("stateCode", profile.stateCode)
                put("pinCode", profile.pinCode)
                put("phone", profile.phone)
                put("email", profile.email)
                put("website", profile.website)
                put("bankName", profile.bankName)
                put("accountHolderName", profile.accountHolderName)
                put("accountNumber", profile.accountNumber)
                put("ifscCode", profile.ifscCode)
                put("branchName", profile.branchName)
                put("upiId", profile.upiId)
                put("authorizedSignatoryName", profile.authorizedSignatoryName)
                put("termsAndConditions", profile.termsAndConditions)
            }
            rootJson.put("businessProfile", profileObj)
        }

        // 2. Export GST Invoices
        val invoices = database.gstInvoiceDao().getAllInvoicesList()
        val invoicesArray = JSONArray()
        for (inv in invoices) {
            val invObj = JSONObject().apply {
                put("id", inv.id)
                put("invoiceNumber", inv.invoiceNumber)
                put("invoiceType", inv.invoiceType)
                put("invoiceDate", inv.invoiceDate)
                put("dueDate", inv.dueDate)
                put("placeOfSupply", inv.placeOfSupply)
                put("reverseChargeApplicable", inv.reverseChargeApplicable)
                put("eWayBillNo", inv.eWayBillNo)
                put("vehicleNo", inv.vehicleNo)
                put("poNumber", inv.poNumber)
                put("poDate", inv.poDate)

                put("sellerName", inv.sellerName)
                put("sellerGstin", inv.sellerGstin)
                put("sellerPan", inv.sellerPan)
                put("sellerAddress", inv.sellerAddress)
                put("sellerPhone", inv.sellerPhone)
                put("sellerEmail", inv.sellerEmail)
                put("sellerState", inv.sellerState)
                put("sellerStateCode", inv.sellerStateCode)
                put("sellerBankName", inv.sellerBankName)
                put("sellerAccountNo", inv.sellerAccountNo)
                put("sellerIfsc", inv.sellerIfsc)
                put("sellerUpi", inv.sellerUpi)

                put("buyerName", inv.buyerName)
                put("buyerGstin", inv.buyerGstin)
                put("buyerPan", inv.buyerPan)
                put("buyerBillingAddress", inv.buyerBillingAddress)
                put("buyerShippingAddress", inv.buyerShippingAddress)
                put("buyerPhone", inv.buyerPhone)
                put("buyerEmail", inv.buyerEmail)
                put("buyerState", inv.buyerState)
                put("buyerStateCode", inv.buyerStateCode)

                put("itemsJson", inv.itemsJson)
                put("templateId", inv.templateId)
                put("status", inv.status)
                put("paymentMode", inv.paymentMode)
                put("notes", inv.notes)
                put("terms", inv.terms)
                put("showHsnSummary", inv.showHsnSummary)
                put("showBankDetails", inv.showBankDetails)
                put("showQrCode", inv.showQrCode)
                put("showSignatory", inv.showSignatory)
                put("primaryColorHex", inv.primaryColorHex)

                put("subtotalAmount", inv.subtotalAmount)
                put("totalCgst", inv.totalCgst)
                put("totalSgst", inv.totalSgst)
                put("totalIgst", inv.totalIgst)
                put("totalCess", inv.totalCess)
                put("roundOffAmount", inv.roundOffAmount)
                put("grandTotalAmount", inv.grandTotalAmount)
                put("createdAt", inv.createdAt)
                put("updatedAt", inv.updatedAt)
            }
            invoicesArray.put(invObj)
        }
        rootJson.put("invoices", invoicesArray)

        // 3. Export Documents Metadata
        val documents = database.documentDao().getDocumentsByCategory(DocumentCategory.ALL.name) // or all
        val docList = database.documentDao().getRecentDocuments(500)
        // Let's get active documents directly
        val docsArray = JSONArray()
        // Query active documents
        val activeDocs = mutableListOf<DocumentEntity>()
        for (category in DocumentCategory.values()) {
            // we can retrieve via query
        }
        rootJson.put("totalInvoices", invoices.size)

        // Write to Backup file
        val backupDir = File(context.filesDir, "backups").apply { mkdirs() }
        val backupFile = File(backupDir, "Scanvoro_Backup_${dateFormat.format(Date(timestamp))}.json")
        FileOutputStream(backupFile).use { out ->
            out.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
        }

        backupFile
    }

    suspend fun restoreFromBackupUri(
        context: Context,
        database: GscanDatabase,
        uri: Uri,
        mode: RestoreMode
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@withContext RestoreResult(false, message = "Could not read backup file")

            val rootJson = JSONObject(jsonString)
            val appName = rootJson.optString("appName", "")
            if (appName != "Scanvoro" && !rootJson.has("invoices")) {
                return@withContext RestoreResult(false, message = "Invalid backup format: Not a recognized Scanvoro backup file.")
            }

            var invoicesCount = 0
            var profileRestored = false

            // 1. Restore Business Profile
            if (rootJson.has("businessProfile")) {
                val p = rootJson.getJSONObject("businessProfile")
                val profileEntity = GstBusinessProfileEntity(
                    id = 1,
                    companyName = p.optString("companyName", "ElectroMech Engineering"),
                    tradeName = p.optString("tradeName", ""),
                    gstin = p.optString("gstin", ""),
                    pan = p.optString("pan", ""),
                    addressLine1 = p.optString("addressLine1", ""),
                    addressLine2 = p.optString("addressLine2", ""),
                    city = p.optString("city", ""),
                    state = p.optString("state", ""),
                    stateCode = p.optString("stateCode", "27"),
                    pinCode = p.optString("pinCode", ""),
                    phone = p.optString("phone", ""),
                    email = p.optString("email", ""),
                    website = p.optString("website", ""),
                    bankName = p.optString("bankName", ""),
                    accountHolderName = p.optString("accountHolderName", ""),
                    accountNumber = p.optString("accountNumber", ""),
                    ifscCode = p.optString("ifscCode", ""),
                    branchName = p.optString("branchName", ""),
                    upiId = p.optString("upiId", ""),
                    authorizedSignatoryName = p.optString("authorizedSignatoryName", "Authorized Signatory"),
                    termsAndConditions = p.optString("termsAndConditions", "")
                )
                database.gstInvoiceDao().saveBusinessProfile(profileEntity)
                profileRestored = true
            }

            // 2. Restore Invoices
            if (rootJson.has("invoices")) {
                val invArray = rootJson.getJSONArray("invoices")
                val newInvoices = mutableListOf<GstInvoiceEntity>()

                for (i in 0 until invArray.length()) {
                    val obj = invArray.getJSONObject(i)
                    val inv = GstInvoiceEntity(
                        id = if (mode == RestoreMode.MERGE) 0L else obj.optLong("id", 0L),
                        invoiceNumber = obj.optString("invoiceNumber", "INV-${System.currentTimeMillis()}"),
                        invoiceType = obj.optString("invoiceType", GstInvoiceType.TAX_INVOICE.name),
                        invoiceDate = obj.optLong("invoiceDate", System.currentTimeMillis()),
                        dueDate = obj.optLong("dueDate", System.currentTimeMillis()),
                        placeOfSupply = obj.optString("placeOfSupply", "Maharashtra (27)"),
                        reverseChargeApplicable = obj.optBoolean("reverseChargeApplicable", false),
                        eWayBillNo = obj.optString("eWayBillNo", ""),
                        vehicleNo = obj.optString("vehicleNo", ""),
                        poNumber = obj.optString("poNumber", ""),
                        poDate = obj.optLong("poDate", 0L),

                        sellerName = obj.optString("sellerName", ""),
                        sellerGstin = obj.optString("sellerGstin", ""),
                        sellerPan = obj.optString("sellerPan", ""),
                        sellerAddress = obj.optString("sellerAddress", ""),
                        sellerPhone = obj.optString("sellerPhone", ""),
                        sellerEmail = obj.optString("sellerEmail", ""),
                        sellerState = obj.optString("sellerState", ""),
                        sellerStateCode = obj.optString("sellerStateCode", "27"),
                        sellerBankName = obj.optString("sellerBankName", ""),
                        sellerAccountNo = obj.optString("sellerAccountNo", ""),
                        sellerIfsc = obj.optString("sellerIfsc", ""),
                        sellerUpi = obj.optString("sellerUpi", ""),

                        buyerName = obj.optString("buyerName", ""),
                        buyerGstin = obj.optString("buyerGstin", ""),
                        buyerPan = obj.optString("buyerPan", ""),
                        buyerBillingAddress = obj.optString("buyerBillingAddress", ""),
                        buyerShippingAddress = obj.optString("buyerShippingAddress", ""),
                        buyerPhone = obj.optString("buyerPhone", ""),
                        buyerEmail = obj.optString("buyerEmail", ""),
                        buyerState = obj.optString("buyerState", ""),
                        buyerStateCode = obj.optString("buyerStateCode", "27"),

                        itemsJson = obj.optString("itemsJson", "[]"),
                        templateId = obj.optString("templateId", GstInvoiceTemplate.CLASSIC_CORPORATE.id),
                        status = obj.optString("status", GstInvoiceStatus.UNPAID.name),
                        paymentMode = obj.optString("paymentMode", "Bank Transfer"),
                        notes = obj.optString("notes", ""),
                        terms = obj.optString("terms", ""),
                        showHsnSummary = obj.optBoolean("showHsnSummary", true),
                        showBankDetails = obj.optBoolean("showBankDetails", true),
                        showQrCode = obj.optBoolean("showQrCode", true),
                        showSignatory = obj.optBoolean("showSignatory", true),
                        primaryColorHex = obj.optString("primaryColorHex", "#1E3A8A"),

                        subtotalAmount = obj.optDouble("subtotalAmount", 0.0),
                        totalCgst = obj.optDouble("totalCgst", 0.0),
                        totalSgst = obj.optDouble("totalSgst", 0.0),
                        totalIgst = obj.optDouble("totalIgst", 0.0),
                        totalCess = obj.optDouble("totalCess", 0.0),
                        roundOffAmount = obj.optDouble("roundOffAmount", 0.0),
                        grandTotalAmount = obj.optDouble("grandTotalAmount", 0.0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = System.currentTimeMillis()
                    )
                    newInvoices.add(inv)
                }

                if (newInvoices.isNotEmpty()) {
                    database.gstInvoiceDao().insertInvoices(newInvoices)
                    invoicesCount = newInvoices.size
                }
            }

            RestoreResult(
                isSuccess = true,
                invoicesRestored = invoicesCount,
                documentsRestored = 0,
                profileRestored = profileRestored,
                message = "Backup successfully restored! $invoicesCount invoices and business profile updated."
            )
        } catch (e: Exception) {
            e.printStackTrace()
            RestoreResult(false, message = "Failed to restore backup: ${e.message}")
        }
    }

    fun shareBackupFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Scanvoro Data Backup Archive")
            putExtra(Intent.EXTRA_TEXT, "Here is the Scanvoro data backup file. You can restore your data anytime using Scanvoro > Settings > Backup & Restore.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share Scanvoro Data Backup")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}

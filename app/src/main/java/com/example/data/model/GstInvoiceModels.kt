package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class GstInvoiceTemplate(val id: String, val displayName: String, val description: String) {
    CLASSIC_CORPORATE("classic", "Classic Corporate", "Formal navy layout with grid borders, HSN summary & banking"),
    MODERN_MINIMAL("modern", "Modern Minimal", "Teal accent, clean cards, sleek typography & badges"),
    INDUSTRIAL_ENGINEERING("industrial", "Industrial & CAD", "Slate blueprint layout with e-way bill, vehicle & tech specs"),
    RETAIL_POS("retail_pos", "Retail POS Bill", "Compact receipt style with tax breakdown & UPI QR placeholder"),
    EXECUTIVE_BURGUNDY("burgundy", "Executive Burgundy", "Deep wine & gold letterhead aesthetic with elegant typography"),
    VIBRANT_COBALT("cobalt", "Vibrant Cobalt", "Dynamic electric blue gradients, modern rounded pill styling")
}

enum class GstInvoiceType(val displayName: String) {
    TAX_INVOICE("Tax Invoice"),
    BILL_OF_SUPPLY("Bill of Supply"),
    PROFORMA_INVOICE("Proforma Invoice"),
    EXPORT_INVOICE("Export Invoice"),
    PURCHASE_ORDER("Purchase Order"),
    QUOTATION("Quotation / Estimate")
}

enum class GstInvoiceStatus(val displayName: String) {
    UNPAID("Unpaid"),
    PAID("Paid"),
    PARTIALLY_PAID("Partially Paid"),
    OVERDUE("Overdue"),
    CANCELLED("Cancelled")
}

data class GstStateInfo(
    val code: String,
    val name: String
)

val INDIAN_GST_STATES = listOf(
    GstStateInfo("01", "Jammu and Kashmir"),
    GstStateInfo("02", "Himachal Pradesh"),
    GstStateInfo("03", "Punjab"),
    GstStateInfo("04", "Chandigarh"),
    GstStateInfo("05", "Uttarakhand"),
    GstStateInfo("06", "Haryana"),
    GstStateInfo("07", "Delhi"),
    GstStateInfo("08", "Rajasthan"),
    GstStateInfo("09", "Uttar Pradesh"),
    GstStateInfo("10", "Bihar"),
    GstStateInfo("11", "Sikkim"),
    GstStateInfo("12", "Arunachal Pradesh"),
    GstStateInfo("13", "Nagaland"),
    GstStateInfo("14", "Manipur"),
    GstStateInfo("15", "Mizoram"),
    GstStateInfo("16", "Tripura"),
    GstStateInfo("17", "Meghalaya"),
    GstStateInfo("18", "Assam"),
    GstStateInfo("19", "West Bengal"),
    GstStateInfo("20", "Jharkhand"),
    GstStateInfo("21", "Odisha"),
    GstStateInfo("22", "Chhattisgarh"),
    GstStateInfo("23", "Madhya Pradesh"),
    GstStateInfo("24", "Gujarat"),
    GstStateInfo("26", "Dadra and Nagar Haveli & Daman and Diu"),
    GstStateInfo("27", "Maharashtra"),
    GstStateInfo("28", "Andhra Pradesh"),
    GstStateInfo("29", "Karnataka"),
    GstStateInfo("30", "Goa"),
    GstStateInfo("31", "Lakshadweep"),
    GstStateInfo("32", "Kerala"),
    GstStateInfo("33", "Tamil Nadu"),
    GstStateInfo("34", "Puducherry"),
    GstStateInfo("35", "Andaman and Nicobar Islands"),
    GstStateInfo("36", "Telangana"),
    GstStateInfo("37", "Andhra Pradesh (New)"),
    GstStateInfo("38", "Ladakh"),
    GstStateInfo("97", "Other Territory")
)

data class GstInvoiceItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val description: String = "",
    val hsnCode: String = "",
    val quantity: Double = 1.0,
    val unit: String = "Pcs",
    val unitPrice: Double = 0.0,
    val discountPercent: Double = 0.0,
    val gstRatePercent: Double = 18.0, // 0, 5, 12, 18, 28
    val cessPercent: Double = 0.0
) {
    val grossAmount: Double get() = quantity * unitPrice
    val discountAmount: Double get() = grossAmount * (discountPercent / 100.0)
    val taxableAmount: Double get() = grossAmount - discountAmount

    fun calculateTaxes(isInterstate: Boolean): ItemTaxResult {
        val tax = taxableAmount * (gstRatePercent / 100.0)
        val cess = taxableAmount * (cessPercent / 100.0)
        return if (isInterstate) {
            ItemTaxResult(
                cgst = 0.0,
                sgst = 0.0,
                igst = tax,
                cess = cess,
                total = taxableAmount + tax + cess
            )
        } else {
            ItemTaxResult(
                cgst = tax / 2.0,
                sgst = tax / 2.0,
                igst = 0.0,
                cess = cess,
                total = taxableAmount + tax + cess
            )
        }
    }
}

data class ItemTaxResult(
    val cgst: Double,
    val sgst: Double,
    val igst: Double,
    val cess: Double,
    val total: Double
)

data class HsnTaxSummary(
    val hsnCode: String,
    val taxableAmount: Double,
    val gstRate: Double,
    val cgst: Double,
    val sgst: Double,
    val igst: Double,
    val cess: Double,
    val totalTax: Double
)

@Entity(tableName = "gst_business_profile")
data class GstBusinessProfileEntity(
    @PrimaryKey val id: Int = 1,
    val companyName: String = "ElectroMech Engineering & Tech Solutions",
    val tradeName: String = "ElectroMech Solutions",
    val gstin: String = "27AAAAA0000A1Z5",
    val pan: String = "AAAAA0000A",
    val addressLine1: String = "Plot No. 42, Industrial Estate, Phase II",
    val addressLine2: String = "MIDC Area, Andheri East",
    val city: String = "Mumbai",
    val state: String = "Maharashtra",
    val stateCode: String = "27",
    val pinCode: String = "400093",
    val phone: String = "+91 98765 43210",
    val email: String = "skelectromech82@gmail.com",
    val website: String = "www.electromechsolutions.com",
    val bankName: String = "State Bank of India",
    val accountHolderName: String = "ElectroMech Engineering & Tech Solutions",
    val accountNumber: String = "38492019384",
    val ifscCode: String = "SBIN0001234",
    val branchName: String = "Industrial Complex Branch",
    val upiId: String = "skelectromech82@sbi",
    val authorizedSignatoryName: String = "Authorized Signatory",
    val termsAndConditions: String = "1. Goods once sold will not be taken back or exchanged.\n2. Interest @ 18% p.a. will be charged if payment is not made within due date.\n3. Subject to Mumbai Jurisdiction only."
)

@Entity(tableName = "gst_invoices")
data class GstInvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String = "INV-2026-0001",
    val invoiceType: String = GstInvoiceType.TAX_INVOICE.name,
    val invoiceDate: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis() + (15L * 24 * 60 * 60 * 1000), // +15 days
    val placeOfSupply: String = "Maharashtra (27)",
    val reverseChargeApplicable: Boolean = false,
    val eWayBillNo: String = "",
    val vehicleNo: String = "",
    val poNumber: String = "",
    val poDate: Long = 0,
    
    // Seller Info
    val sellerName: String = "ElectroMech Engineering & Tech Solutions",
    val sellerGstin: String = "27AAAAA0000A1Z5",
    val sellerPan: String = "AAAAA0000A",
    val sellerAddress: String = "Plot No. 42, Industrial Estate, Phase II, MIDC Area, Andheri East, Mumbai 400093",
    val sellerPhone: String = "+91 98765 43210",
    val sellerEmail: String = "skelectromech82@gmail.com",
    val sellerState: String = "Maharashtra",
    val sellerStateCode: String = "27",
    val sellerBankName: String = "State Bank of India",
    val sellerAccountNo: String = "38492019384",
    val sellerIfsc: String = "SBIN0001234",
    val sellerUpi: String = "skelectromech82@sbi",

    // Buyer / Client Info
    val buyerName: String = "Precision Automation Works Pvt Ltd",
    val buyerGstin: String = "27BBBBB1111B1Z2",
    val buyerPan: String = "BBBBB1111B",
    val buyerBillingAddress: String = "Unit 102, Tech Park, Kothrud, Pune, Maharashtra 411038",
    val buyerShippingAddress: String = "Unit 102, Tech Park, Kothrud, Pune, Maharashtra 411038",
    val buyerPhone: String = "+91 91234 56789",
    val buyerEmail: String = "accounts@precisionauto.in",
    val buyerState: String = "Maharashtra",
    val buyerStateCode: String = "27",

    // Serialized Items JSON
    val itemsJson: String = "[]",

    // Customization & Template
    val templateId: String = GstInvoiceTemplate.CLASSIC_CORPORATE.id,
    val status: String = GstInvoiceStatus.UNPAID.name,
    val paymentMode: String = "Bank Transfer / NEFT",
    val notes: String = "Thank you for your business. Please quote invoice number on payment advice.",
    val terms: String = "1. Goods once sold will not be taken back.\n2. Payment due within 15 days of invoice date.\n3. Subject to local jurisdiction only.",
    val showHsnSummary: Boolean = true,
    val showBankDetails: Boolean = true,
    val showQrCode: Boolean = true,
    val showSignatory: Boolean = true,
    val primaryColorHex: String = "#1E3A8A", // Navy default

    // Calculated snapshot values
    val subtotalAmount: Double = 0.0,
    val totalCgst: Double = 0.0,
    val totalSgst: Double = 0.0,
    val totalIgst: Double = 0.0,
    val totalCess: Double = 0.0,
    val roundOffAmount: Double = 0.0,
    val grandTotalAmount: Double = 0.0,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

object GstNumberToWords {
    private val units = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    )
    private val tens = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    fun convert(amount: Double): String {
        if (amount == 0.0) return "Zero Rupees Only"

        val wholePart = amount.toLong()
        val paisePart = Math.round((amount - wholePart) * 100).toInt()

        val words = convertIndianNumber(wholePart)
        val paiseWords = if (paisePart > 0) " and " + convertIndianNumber(paisePart.toLong()) + " Paise" else ""

        return "Rupees $words$paiseWords Only"
    }

    private fun convertIndianNumber(n: Long): String {
        if (n == 0L) return "Zero"

        var num = n
        var result = ""

        if (num >= 10000000) { // Crores
            val crores = num / 10000000
            result += convertLessThanThousand(crores) + " Crore "
            num %= 10000000
        }

        if (num >= 100000) { // Lakhs
            val lakhs = num / 100000
            result += convertLessThanThousand(lakhs) + " Lakh "
            num %= 100000
        }

        if (num >= 1000) { // Thousands
            val thousands = num / 1000
            result += convertLessThanThousand(thousands) + " Thousand "
            num %= 1000
        }

        if (num > 0) {
            result += convertLessThanThousand(num)
        }

        return result.trim()
    }

    private fun convertLessThanThousand(n: Long): String {
        var num = n
        var res = ""

        if (num >= 100) {
            res += units[(num / 100).toInt()] + " Hundred "
            num %= 100
        }

        if (num >= 20) {
            res += tens[(num / 10).toInt()] + " "
            num %= 10
        }

        if (num > 0) {
            res += units[num.toInt()] + " "
        }

        return res.trim()
    }
}

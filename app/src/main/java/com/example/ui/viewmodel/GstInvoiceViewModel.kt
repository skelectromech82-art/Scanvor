package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.GscanDatabase
import com.example.data.model.*
import com.example.data.repository.DocumentRepository
import com.example.data.repository.GstInvoiceRepository
import com.example.utils.GstInvoicePdfEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class GstInvoiceUiState(
    val currentInvoice: GstInvoiceEntity = GstInvoiceEntity(),
    val currentItems: List<GstInvoiceItem> = listOf(
        GstInvoiceItem(
            description = "Electrical Control Panel Assembly 415V",
            hsnCode = "8537",
            quantity = 2.0,
            unit = "Set",
            unitPrice = 18500.0,
            discountPercent = 5.0,
            gstRatePercent = 18.0
        ),
        GstInvoiceItem(
            description = "Copper Armoured Power Cable 3.5 Core 50 Sq.mm",
            hsnCode = "8544",
            quantity = 50.0,
            unit = "Mtr",
            unitPrice = 420.0,
            discountPercent = 0.0,
            gstRatePercent = 18.0
        ),
        GstInvoiceItem(
            description = "On-site Installation & CAD Testing Service",
            hsnCode = "9987",
            quantity = 1.0,
            unit = "Job",
            unitPrice = 6500.0,
            discountPercent = 0.0,
            gstRatePercent = 18.0
        )
    ),
    val selectedTemplate: GstInvoiceTemplate = GstInvoiceTemplate.CLASSIC_CORPORATE,
    val savedInvoices: List<GstInvoiceEntity> = emptyList(),
    val businessProfile: GstBusinessProfileEntity = GstBusinessProfileEntity(),
    val searchQuery: String = "",
    val filterStatus: String = "ALL",
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val exportedPdfFile: File? = null
)

class GstInvoiceViewModel(application: Application) : AndroidViewModel(application) {
    private val database = GscanDatabase.getDatabase(application)
    val invoiceRepository = GstInvoiceRepository(application, database.gstInvoiceDao())
    val documentRepository = DocumentRepository(application, database.documentDao())

    private val _uiState = MutableStateFlow(GstInvoiceUiState())
    val uiState: StateFlow<GstInvoiceUiState> = _uiState.asStateFlow()

    init {
        loadBusinessProfile()
        observeInvoices()
    }

    private fun loadBusinessProfile() {
        viewModelScope.launch {
            val profile = invoiceRepository.getBusinessProfile()
            _uiState.update { state ->
                state.copy(
                    businessProfile = profile,
                    currentInvoice = state.currentInvoice.copy(
                        sellerName = profile.companyName,
                        sellerGstin = profile.gstin,
                        sellerPan = profile.pan,
                        sellerAddress = "${profile.addressLine1}, ${profile.city} ${profile.pinCode}",
                        sellerPhone = profile.phone,
                        sellerEmail = profile.email,
                        sellerState = profile.state,
                        sellerStateCode = profile.stateCode,
                        sellerBankName = profile.bankName,
                        sellerAccountNo = profile.accountNumber,
                        sellerIfsc = profile.ifscCode,
                        sellerUpi = profile.upiId,
                        terms = profile.termsAndConditions
                    )
                )
            }
        }
    }

    private fun observeInvoices() {
        viewModelScope.launch {
            invoiceRepository.allInvoices.collect { list ->
                _uiState.update { it.copy(savedInvoices = list) }
            }
        }
    }

    fun updateInvoiceDetails(
        invoiceNumber: String? = null,
        invoiceType: String? = null,
        invoiceDate: Long? = null,
        dueDate: Long? = null,
        placeOfSupply: String? = null,
        buyerName: String? = null,
        buyerGstin: String? = null,
        buyerPan: String? = null,
        buyerBillingAddress: String? = null,
        buyerShippingAddress: String? = null,
        buyerPhone: String? = null,
        buyerEmail: String? = null,
        buyerState: String? = null,
        buyerStateCode: String? = null,
        eWayBillNo: String? = null,
        vehicleNo: String? = null,
        poNumber: String? = null,
        notes: String? = null,
        terms: String? = null,
        status: String? = null
    ) {
        _uiState.update { state ->
            val current = state.currentInvoice
            state.copy(
                currentInvoice = current.copy(
                    invoiceNumber = invoiceNumber ?: current.invoiceNumber,
                    invoiceType = invoiceType ?: current.invoiceType,
                    invoiceDate = invoiceDate ?: current.invoiceDate,
                    dueDate = dueDate ?: current.dueDate,
                    placeOfSupply = placeOfSupply ?: current.placeOfSupply,
                    buyerName = buyerName ?: current.buyerName,
                    buyerGstin = buyerGstin ?: current.buyerGstin,
                    buyerPan = buyerPan ?: current.buyerPan,
                    buyerBillingAddress = buyerBillingAddress ?: current.buyerBillingAddress,
                    buyerShippingAddress = buyerShippingAddress ?: current.buyerShippingAddress,
                    buyerPhone = buyerPhone ?: current.buyerPhone,
                    buyerEmail = buyerEmail ?: current.buyerEmail,
                    buyerState = buyerState ?: current.buyerState,
                    buyerStateCode = buyerStateCode ?: current.buyerStateCode,
                    eWayBillNo = eWayBillNo ?: current.eWayBillNo,
                    vehicleNo = vehicleNo ?: current.vehicleNo,
                    poNumber = poNumber ?: current.poNumber,
                    notes = notes ?: current.notes,
                    terms = terms ?: current.terms,
                    status = status ?: current.status
                )
            )
        }
    }

    fun setTemplate(template: GstInvoiceTemplate) {
        _uiState.update {
            it.copy(
                selectedTemplate = template,
                currentInvoice = it.currentInvoice.copy(templateId = template.id)
            )
        }
    }

    fun addItem(item: GstInvoiceItem) {
        _uiState.update { state ->
            state.copy(currentItems = state.currentItems + item)
        }
    }

    fun updateItem(index: Int, item: GstInvoiceItem) {
        _uiState.update { state ->
            val updated = state.currentItems.toMutableList()
            if (index in updated.indices) {
                updated[index] = item
            }
            state.copy(currentItems = updated)
        }
    }

    fun removeItem(index: Int) {
        _uiState.update { state ->
            val updated = state.currentItems.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            state.copy(currentItems = updated)
        }
    }

    fun saveBusinessProfile(profile: GstBusinessProfileEntity) {
        viewModelScope.launch {
            invoiceRepository.saveBusinessProfile(profile)
            _uiState.update {
                it.copy(
                    businessProfile = profile,
                    userMessage = "Business profile saved successfully"
                )
            }
        }
    }

    fun saveCurrentInvoice(onSaved: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val state = _uiState.value
                val isInterstate = state.currentInvoice.sellerStateCode.trim() != state.currentInvoice.buyerStateCode.trim()

                var subtotal = 0.0
                var totalCgst = 0.0
                var totalSgst = 0.0
                var totalIgst = 0.0
                var totalCess = 0.0

                for (item in state.currentItems) {
                    val tax = item.calculateTaxes(isInterstate)
                    subtotal += item.taxableAmount
                    totalCgst += tax.cgst
                    totalSgst += tax.sgst
                    totalIgst += tax.igst
                    totalCess += tax.cess
                }

                val rawTotal = subtotal + totalCgst + totalSgst + totalIgst + totalCess
                val grandTotal = Math.round(rawTotal * 100.0) / 100.0
                val roundOff = Math.round((Math.round(grandTotal) - grandTotal) * 100.0) / 100.0
                val finalPayable = Math.round(grandTotal).toDouble()

                val invoiceToSave = state.currentInvoice.copy(
                    itemsJson = GstInvoiceRepository.serializeItems(state.currentItems),
                    templateId = state.selectedTemplate.id,
                    subtotalAmount = subtotal,
                    totalCgst = totalCgst,
                    totalSgst = totalSgst,
                    totalIgst = totalIgst,
                    totalCess = totalCess,
                    roundOffAmount = roundOff,
                    grandTotalAmount = finalPayable,
                    updatedAt = System.currentTimeMillis()
                )

                val docId = invoiceRepository.saveInvoice(invoiceToSave)
                _uiState.update {
                    it.copy(
                        currentInvoice = invoiceToSave.copy(id = docId),
                        isLoading = false,
                        userMessage = "Invoice #${invoiceToSave.invoiceNumber} saved successfully!"
                    )
                }
                onSaved?.invoke(docId)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Error saving invoice: ${e.message}") }
            }
        }
    }

    fun exportPdf(context: Context, onExported: (File) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val state = _uiState.value
                val pdfFile = GstInvoicePdfEngine.generateInvoicePdf(
                    context = context,
                    invoice = state.currentInvoice,
                    items = state.currentItems
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        exportedPdfFile = pdfFile,
                        userMessage = "PDF Invoice created: ${pdfFile.name}"
                    )
                }
                onExported(pdfFile)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Failed to export PDF: ${e.message}") }
            }
        }
    }

    fun loadInvoice(invoiceId: Long) {
        viewModelScope.launch {
            val invoice = invoiceRepository.getInvoiceById(invoiceId)
            if (invoice != null) {
                val items = GstInvoiceRepository.deserializeItems(invoice.itemsJson)
                val template = GstInvoiceTemplate.values().firstOrNull { it.id == invoice.templateId } ?: GstInvoiceTemplate.CLASSIC_CORPORATE
                _uiState.update {
                    it.copy(
                        currentInvoice = invoice,
                        currentItems = if (items.isNotEmpty()) items else it.currentItems,
                        selectedTemplate = template
                    )
                }
            }
        }
    }

    fun resetNewInvoice() {
        val timestamp = System.currentTimeMillis()
        val randomNum = (1000..9999).random()
        val profile = _uiState.value.businessProfile

        _uiState.update { state ->
            state.copy(
                currentInvoice = GstInvoiceEntity(
                    invoiceNumber = "INV-2026-$randomNum",
                    sellerName = profile.companyName,
                    sellerGstin = profile.gstin,
                    sellerPan = profile.pan,
                    sellerAddress = "${profile.addressLine1}, ${profile.city} ${profile.pinCode}",
                    sellerPhone = profile.phone,
                    sellerEmail = profile.email,
                    sellerState = profile.state,
                    sellerStateCode = profile.stateCode,
                    sellerBankName = profile.bankName,
                    sellerAccountNo = profile.accountNumber,
                    sellerIfsc = profile.ifscCode,
                    sellerUpi = profile.upiId,
                    terms = profile.termsAndConditions
                ),
                currentItems = listOf(
                    GstInvoiceItem(
                        description = "Industrial Goods / Service",
                        hsnCode = "8479",
                        quantity = 1.0,
                        unit = "Pcs",
                        unitPrice = 5000.0,
                        gstRatePercent = 18.0
                    )
                )
            )
        }
    }

    fun deleteInvoice(id: Long) {
        viewModelScope.launch {
            invoiceRepository.deleteInvoice(id)
            _uiState.update { it.copy(userMessage = "Invoice deleted") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}

package com.example.data.repository

import android.content.Context
import com.example.data.local.GstInvoiceDao
import com.example.data.model.GstBusinessProfileEntity
import com.example.data.model.GstInvoiceEntity
import com.example.data.model.GstInvoiceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class GstInvoiceRepository(
    private val context: Context,
    private val invoiceDao: GstInvoiceDao
) {
    val allInvoices: Flow<List<GstInvoiceEntity>> = invoiceDao.getAllInvoices()
    val businessProfile: Flow<GstBusinessProfileEntity?> = invoiceDao.getBusinessProfileFlow()

    fun getInvoicesByStatus(status: String): Flow<List<GstInvoiceEntity>> =
        invoiceDao.getInvoicesByStatus(status)

    fun searchInvoices(query: String): Flow<List<GstInvoiceEntity>> =
        invoiceDao.searchInvoices(query)

    fun getInvoiceFlow(id: Long): Flow<GstInvoiceEntity?> =
        invoiceDao.getInvoiceFlow(id)

    suspend fun getInvoiceById(id: Long): GstInvoiceEntity? = withContext(Dispatchers.IO) {
        invoiceDao.getInvoiceById(id)
    }

    suspend fun getAllInvoicesList(): List<GstInvoiceEntity> = withContext(Dispatchers.IO) {
        invoiceDao.getAllInvoicesList()
    }

    suspend fun getBusinessProfile(): GstBusinessProfileEntity = withContext(Dispatchers.IO) {
        invoiceDao.getBusinessProfile() ?: GstBusinessProfileEntity().also {
            invoiceDao.saveBusinessProfile(it)
        }
    }

    suspend fun saveBusinessProfile(profile: GstBusinessProfileEntity) = withContext(Dispatchers.IO) {
        invoiceDao.saveBusinessProfile(profile)
    }

    suspend fun saveInvoice(invoice: GstInvoiceEntity): Long = withContext(Dispatchers.IO) {
        if (invoice.id == 0L) {
            invoiceDao.insertInvoice(invoice)
        } else {
            invoiceDao.updateInvoice(invoice.copy(updatedAt = System.currentTimeMillis()))
            invoice.id
        }
    }

    suspend fun insertInvoices(invoices: List<GstInvoiceEntity>) = withContext(Dispatchers.IO) {
        invoiceDao.insertInvoices(invoices)
    }

    suspend fun deleteInvoice(id: Long) = withContext(Dispatchers.IO) {
        invoiceDao.deleteInvoiceById(id)
    }

    suspend fun updateInvoiceStatus(id: Long, status: String) = withContext(Dispatchers.IO) {
        invoiceDao.updateInvoiceStatus(id, status)
    }

    companion object {
        fun serializeItems(items: List<GstInvoiceItem>): String {
            val jsonArray = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("description", item.description)
                    put("hsnCode", item.hsnCode)
                    put("quantity", item.quantity)
                    put("unit", item.unit)
                    put("unitPrice", item.unitPrice)
                    put("discountPercent", item.discountPercent)
                    put("gstRatePercent", item.gstRatePercent)
                    put("cessPercent", item.cessPercent)
                }
                jsonArray.put(obj)
            }
            return jsonArray.toString()
        }

        fun deserializeItems(json: String): List<GstInvoiceItem> {
            if (json.isBlank()) return emptyList()
            val list = mutableListOf<GstInvoiceItem>()
            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        GstInvoiceItem(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            description = obj.optString("description", ""),
                            hsnCode = obj.optString("hsnCode", ""),
                            quantity = obj.optDouble("quantity", 1.0),
                            unit = obj.optString("unit", "Pcs"),
                            unitPrice = obj.optDouble("unitPrice", 0.0),
                            discountPercent = obj.optDouble("discountPercent", 0.0),
                            gstRatePercent = obj.optDouble("gstRatePercent", 18.0),
                            cessPercent = obj.optDouble("cessPercent", 0.0)
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return list
        }
    }
}

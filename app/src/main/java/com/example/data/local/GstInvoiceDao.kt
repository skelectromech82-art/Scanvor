package com.example.data.local

import androidx.room.*
import com.example.data.model.GstBusinessProfileEntity
import com.example.data.model.GstInvoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GstInvoiceDao {
    @Query("SELECT * FROM gst_invoices ORDER BY updatedAt DESC")
    fun getAllInvoices(): Flow<List<GstInvoiceEntity>>

    @Query("SELECT * FROM gst_invoices ORDER BY updatedAt DESC")
    suspend fun getAllInvoicesList(): List<GstInvoiceEntity>

    @Query("SELECT * FROM gst_invoices WHERE id = :id")
    fun getInvoiceFlow(id: Long): Flow<GstInvoiceEntity?>

    @Query("SELECT * FROM gst_invoices WHERE id = :id")
    suspend fun getInvoiceById(id: Long): GstInvoiceEntity?

    @Query("SELECT * FROM gst_invoices WHERE status = :status ORDER BY updatedAt DESC")
    fun getInvoicesByStatus(status: String): Flow<List<GstInvoiceEntity>>

    @Query("SELECT * FROM gst_invoices WHERE invoiceNumber LIKE '%' || :query || '%' OR buyerName LIKE '%' || :query || '%' OR buyerGstin LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchInvoices(query: String): Flow<List<GstInvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: GstInvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoices(invoices: List<GstInvoiceEntity>)

    @Update
    suspend fun updateInvoice(invoice: GstInvoiceEntity)

    @Delete
    suspend fun deleteInvoice(invoice: GstInvoiceEntity)

    @Query("DELETE FROM gst_invoices WHERE id = :id")
    suspend fun deleteInvoiceById(id: Long)

    @Query("UPDATE gst_invoices SET status = :status, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateInvoiceStatus(id: Long, status: String, timestamp: Long = System.currentTimeMillis())

    // Business Profile
    @Query("SELECT * FROM gst_business_profile WHERE id = 1")
    fun getBusinessProfileFlow(): Flow<GstBusinessProfileEntity?>

    @Query("SELECT * FROM gst_business_profile WHERE id = 1")
    suspend fun getBusinessProfile(): GstBusinessProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBusinessProfile(profile: GstBusinessProfileEntity)
}

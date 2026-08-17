package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentPage
import com.example.data.model.GstBusinessProfileEntity
import com.example.data.model.GstInvoiceEntity

@Database(
    entities = [
        DocumentEntity::class,
        DocumentPage::class,
        GstInvoiceEntity::class,
        GstBusinessProfileEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class GscanDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun gstInvoiceDao(): GstInvoiceDao

    companion object {
        @Volatile
        private var INSTANCE: GscanDatabase? = null

        fun getDatabase(context: Context): GscanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GscanDatabase::class.java,
                    "gscan_master_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

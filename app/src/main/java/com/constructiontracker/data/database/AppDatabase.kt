package com.constructiontracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.constructiontracker.data.database.dao.ContractorDao
import com.constructiontracker.data.database.dao.PaymentDao
import com.constructiontracker.data.database.dao.PurchaseDao
import com.constructiontracker.data.database.entities.ContractorEntity
import com.constructiontracker.data.database.entities.PaymentEntity
import com.constructiontracker.data.database.entities.PurchaseEntity

@Database(
    entities = [ContractorEntity::class, PaymentEntity::class, PurchaseEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contractorDao(): ContractorDao
    abstract fun paymentDao(): PaymentDao
    abstract fun purchaseDao(): PurchaseDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE contractors ADD COLUMN contactNumber TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE contractors ADD COLUMN bankAccountNumber TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE contractors ADD COLUMN bankName TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE contractors ADD COLUMN bankBranch TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE contractors ADD COLUMN photoUri TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "construction_tracker_db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
    }
}

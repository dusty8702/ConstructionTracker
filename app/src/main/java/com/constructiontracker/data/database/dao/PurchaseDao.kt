package com.constructiontracker.data.database.dao

import androidx.room.*
import com.constructiontracker.data.database.entities.PurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases ORDER BY date DESC, createdAt DESC")
    fun getAllPurchases(): Flow<List<PurchaseEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM purchases")
    fun getTotalPurchases(): Flow<Double>

    @Insert
    suspend fun insertPurchase(purchase: PurchaseEntity)

    @Delete
    suspend fun deletePurchase(purchase: PurchaseEntity)
}
